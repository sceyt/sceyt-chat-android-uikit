package com.sceyt.chatuikit.presentation.components.picker

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore.Files
import android.provider.MediaStore.Images
import android.provider.MediaStore.Video
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker.MediaType
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker.PickerFilterType
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker.SelectedMediaData
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaData
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class MediaPickerState(
    val items: List<MediaItem> = emptyList(),
    val selectedCount: Int = 0,
    val isLoaded: Boolean = false,
)

/**
 * Loads gallery media for [BottomSheetMediaPicker] once, refreshes it when the gallery changes,
 * and keeps the user's selection across configuration changes and process death.
 */
class MediaPickerViewModel(
    private val contentResolver: ContentResolver,
    private val savedStateHandle: SavedStateHandle,
    private val filterType: PickerFilterType,
    val maxSelectCount: Int,
    preselectedPaths: List<String>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _state = MutableStateFlow(MediaPickerState())
    val state: StateFlow<MediaPickerState> = _state.asStateFlow()

    private val selected = LinkedHashMap<String, MediaData>()
    private val pendingPaths = savedStateHandle.get<ArrayList<String>>(KEY_SELECTED_PATHS)
        ?.toMutableSet() ?: preselectedPaths.toMutableSet()
    private var loadJob: Job? = null
    private var isLoadStarted = false
    private var loadedSnapshot: GallerySnapshot? = null

    private val galleryObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            reload()
        }
    }

    fun loadMedia() {
        if (isLoadStarted) return
        isLoadStarted = true
        contentResolver.registerContentObserver(QUERY_URI, true, galleryObserver)
        reload()
    }

    fun refreshIfGalleryChanged() {
        if (!isLoadStarted || loadJob?.isActive == true) return
        if (!_state.value.isLoaded) {
            reload()
            return
        }

        viewModelScope.launch {
            val snapshot = withContext(ioDispatcher) {
                queryGallerySnapshot()
            }
            if (loadJob?.isActive == true) return@launch

            if (snapshot != null && snapshot != loadedSnapshot) {
                reload()
            }
        }
    }

    fun toggleSelection(media: MediaData): Boolean {
        if (!media.selected && selected.size >= maxSelectCount) return false

        media.selected = !media.selected
        if (media.selected) {
            selected[media.realPath] = media
        } else {
            selected.remove(media.realPath)
        }
        onSelectionChanged()
        return true
    }

    fun getSelectedMedia(): List<SelectedMediaData> {
        return selected.values.map {
            SelectedMediaData(it.contentUri, it.realPath, it.mediaType)
        }
    }

    private fun reload() {
        loadJob?.cancel()
        val isFirstLoad = !_state.value.isLoaded
        loadJob = viewModelScope.launch {
            val items = ArrayList<MediaItem>()
            var isFailed = false

            queryMedia().catch {
                isFailed = true
                SceytLog.e(this@MediaPickerViewModel.TAG, it.message.toString())
            }.collect { chunk ->
                items.addAll(chunk)
                if (isFirstLoad) {
                    publish(newItems = chunk, allItems = items)
                }
            }
            if (isFailed) return@launch

            val loadedPaths = items
                .filterNot { it.media.isWrong }
                .mapTo(HashSet()) { it.media.realPath }
            selected.keys.retainAll(loadedPaths)
            if (isFirstLoad) {
                onSelectionChanged()
            } else {
                publish(newItems = items, allItems = items)
            }
            loadedSnapshot = items.toSnapshot()
            _state.update { it.copy(isLoaded = true) }
        }
    }

    private fun List<MediaItem>.toSnapshot(): GallerySnapshot {
        val idSum = sumOf { ContentUris.parseId(it.media.contentUri) }
        return GallerySnapshot(count = size, idSum = idSum)
    }

    private fun queryGallerySnapshot(): GallerySnapshot? {
        return try {
            val selection = getSelection(filterType)
            val cursor = contentResolver.query(QUERY_URI, SNAPSHOT_PROJECTION, selection, null, null)
            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(Files.FileColumns._ID)
                var idSum = 0L
                while (it.moveToNext()) {
                    idSum += it.getLong(idIndex)
                }
                GallerySnapshot(count = it.count, idSum = idSum)
            }
        } catch (e: Exception) {
            SceytLog.e(TAG, e.message.toString())
            null
        }
    }

    private fun publish(newItems: List<MediaItem>, allItems: List<MediaItem>) {
        newItems.forEach { applySelection(it.media) }
        _state.update { it.copy(items = allItems.toList()) }
        onSelectionChanged()
    }

    private fun applySelection(media: MediaData) {
        val path = media.realPath
        media.selected = pendingPaths.remove(path) || selected.containsKey(path)
        if (media.selected) {
            selected[path] = media
        }
    }

    private fun onSelectionChanged() {
        _state.update { it.copy(selectedCount = selected.size) }
        savedStateHandle[KEY_SELECTED_PATHS] = ArrayList(selected.keys + pendingPaths)
    }

    private fun queryMedia(): Flow<List<MediaItem>> = flow {
        val selection = getSelection(filterType)
        val cursor = contentResolver.query(QUERY_URI, PROJECTION, selection, null, SORT_ORDER)
            ?: error("Gallery query returned no cursor")
        cursor.use {
            val items = ArrayList<MediaItem>()
            val wrongImages = ArrayList<MediaItem>()
            val columnIndex = cursor.getColumnIndex(Files.FileColumns._ID)
            val columnMediaTypeIndex = cursor.getColumnIndex(Files.FileColumns.MEDIA_TYPE)
            val columnDataIndex = cursor.getColumnIndex(Files.FileColumns.DATA)
            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val id = cursor.getLong(columnIndex)
                val type = cursor.getInt(columnMediaTypeIndex)
                var isImage: Boolean
                var videoDuration = 0.0

                val contentUri = if (type == Files.FileColumns.MEDIA_TYPE_VIDEO) {
                    isImage = false

                    val durationIndex = cursor.getColumnIndex(Video.Media.DURATION)
                    videoDuration = cursor.getDouble(durationIndex)
                    ContentUris.withAppendedId(Video.Media.EXTERNAL_CONTENT_URI, id)

                } else {
                    isImage = true
                    ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, id)
                }

                val realPath = cursor.getString(columnDataIndex)
                val isWrong = !File(realPath).exists()

                val mediaType = if (isImage) MediaType.Image else MediaType.Video
                val model = MediaData(contentUri, realPath, isWrong, mediaType = mediaType)
                val mediaItem = if (isImage)
                    MediaItem.Image(model) else MediaItem.Video(model, videoDuration)

                if (isWrong)
                    wrongImages.add(mediaItem)
                else
                    items.add(mediaItem)

                if (items.size == CHUNK_SIZE) {
                    emit(ArrayList(items))
                    items.clear()
                }
            }
            emit(items + wrongImages)
        }
    }.flowOn(ioDispatcher)

    private fun getSelection(filter: PickerFilterType): String {
        val selectionImage = Files.FileColumns.MEDIA_TYPE + "=" + Files.FileColumns.MEDIA_TYPE_IMAGE
        val selectionVideo = Files.FileColumns.MEDIA_TYPE + "=" + Files.FileColumns.MEDIA_TYPE_VIDEO
        val selectionAll = "$selectionImage OR $selectionVideo"

        return when (filter) {
            PickerFilterType.All -> selectionAll
            PickerFilterType.Image -> selectionImage
            PickerFilterType.Video -> selectionVideo
        }
    }

    override fun onCleared() {
        contentResolver.unregisterContentObserver(galleryObserver)
    }

    private data class GallerySnapshot(val count: Int, val idSum: Long)

    private companion object {
        const val CHUNK_SIZE = 150
        const val KEY_SELECTED_PATHS = "selectedPaths"
        const val SORT_ORDER = Video.Media.DATE_ADDED + " DESC"
        val QUERY_URI: Uri = Files.getContentUri("external")
        val PROJECTION = arrayOf(
            Files.FileColumns._ID,
            Files.FileColumns.DATA,
            Files.FileColumns.DATE_ADDED,
            Files.FileColumns.MEDIA_TYPE,
            Video.Media.DURATION
        )
        val SNAPSHOT_PROJECTION = arrayOf(Files.FileColumns._ID)
    }
}
