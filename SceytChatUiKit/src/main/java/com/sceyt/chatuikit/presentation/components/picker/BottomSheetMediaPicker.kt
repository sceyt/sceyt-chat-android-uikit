package com.sceyt.chatuikit.presentation.components.picker

import android.content.ContentUris
import android.content.Context
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytBottomSheetMediaPickerBinding
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.checkAndAskPermissions
import com.sceyt.chatuikit.extensions.dismissSafety
import com.sceyt.chatuikit.extensions.getOrientation
import com.sceyt.chatuikit.extensions.getPermissionsForMangeStorage
import com.sceyt.chatuikit.extensions.initPermissionLauncher
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.screenHeightPx
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaData
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaItem
import com.sceyt.chatuikit.presentation.components.picker.adapter.holders.MediaPickerItemViewHolderFactory
import com.sceyt.chatuikit.styles.MediaPickerStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class BottomSheetMediaPicker : BottomSheetDialogFragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private lateinit var binding: SceytBottomSheetMediaPickerBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private val selectedMedia = mutableSetOf<MediaData>()
    private var selectedMediaPaths = mutableSetOf<String>()
    private var requestedSelectionMediaPaths = mutableSetOf<String>()
    private val screenHeight by lazy { screenHeightPx() }
    private val peekHeight by lazy { screenHeight / 1.5 }
    private var maxSelectCount: Int = MAX_SELECT_MEDIA_COUNT
    private var filterType: PickerFilterType = PickerFilterType.All
    private lateinit var style: MediaPickerStyle
    private val imagesAdapter by lazy {
        MediaAdapter(initGalleryViewHolderFactory(), style)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.getStringArray(STATE_SELECTION)?.let {
            selectedMediaPaths = it.filter { path -> path.isNotNullOrBlank() }.toMutableSet()
        } ?: run {
            arguments?.getStringArray(STATE_SELECTION)?.let {
                requestedSelectionMediaPaths = it.filter { path -> path.isNotNullOrBlank() }.toMutableSet()
            }
        }

        arguments?.getInt(MAX_SELECTION_COUNT)?.let {
            maxSelectCount = it
        }

        arguments?.getInt(FILTER_TYPE)?.let {
            filterType = PickerFilterType.entries[it]
        }

        checkPermissions {
            if (it) LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
        }
    }

    private fun initGalleryViewHolderFactory(): MediaPickerItemViewHolderFactory {
        val clickListener = MediaAdapter.MediaClickListener(::onMediaClick)
        return MediaPickerItemViewHolderFactory(style, clickListener)
    }

    private fun checkPermissions(callBack: (Boolean) -> Unit) {
        val resultLauncher = initPermissionLauncher {
            if (it) callBack.invoke(true)
        }

        val permissions = getPermissionsForMangeStorage()
        val hasAccess = requireContext().checkAndAskPermissions(resultLauncher, *permissions)

        if (hasAccess)
            callBack.invoke(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytBottomSheetMediaPickerBinding.inflate(inflater, container, false)
            .also {
                binding = it
                binding.applyStyle()
            }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.rvMedia) {
            val spanCount = if (requireContext().getOrientation() == Configuration.ORIENTATION_LANDSCAPE)
                5 else 3

            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), spanCount)
            adapter = imagesAdapter
        }

        binding.initViews()
        setCounter()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            super.onCreateDialog(savedInstanceState).apply {
                setOnShowListener {
                    val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                    bottomSheetBehavior.peekHeight = peekHeight.toInt()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    binding.layoutCounter.translationY = -((bottomSheet.height - peekHeight).toFloat())
                    bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
                }
            }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = MediaPickerStyle.Builder(context, null).build()
    }

    private fun SceytBottomSheetMediaPickerBinding.initViews() {
        btnNext.setOnClickListener {
            pickerListener?.onSelect(selectedMedia.map {
                SelectedMediaData(it.contentUri, it.realPath, it.mediaType)
            })
            pickerListener = null
            dismissSafety()
        }
    }

    private fun onMediaClick(mediaItem: MediaItem, position: Int) {
        val item = mediaItem.media
        if (selectedMedia.size == maxSelectCount && item.selected.not()) {
            Toast.makeText(requireContext(), "${context?.getString(R.string.sceyt_you_can_select_max)} " +
                    "$maxSelectCount", Toast.LENGTH_SHORT).show()
            return
        }
        item.selected = !item.selected
        imagesAdapter.notifyItemChanged(position, GalleryMediaItemDiff.DEFAULT.copy(filePathChanged = false))

        if (item.selected) {
            selectedMedia.add(item)
            selectedMediaPaths.add(item.realPath)
        } else {
            selectedMedia.remove(item)
            selectedMediaPaths.remove(item.realPath)
        }

        setCounter()
    }

    private fun setCounter() {
        binding.counter.isVisible = selectedMediaPaths.size > 0
        binding.counter.text = selectedMediaPaths.size.toString()
    }

    private val bottomSheetCallback by lazy {
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val bottomSheetVisibleHeight = bottomSheet.height - bottomSheet.top
                if (bottomSheetVisibleHeight >= peekHeight)
                    binding.layoutCounter.translationY = -((bottomSheet.height - bottomSheetVisibleHeight).toFloat())
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    dismissAllowingStateLoss()
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        if (id != LOADER_ID) throw IllegalStateException("illegal loader id: $id")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Video.Media.DURATION
        )
        val sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC"
        val selection = getSelection(filterType)
        val queryUri = MediaStore.Files.getContentUri("external")

        return CursorLoader(requireContext(), queryUri, projection, selection, null, sortOrder)
    }

    private fun getSelection(filter: PickerFilterType): String {
        val selectionImage = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        val selectionVideo = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        val selectionAll = "$selectionImage OR $selectionVideo"

        return when (filter) {
            PickerFilterType.All -> selectionAll
            PickerFilterType.Image -> selectionImage
            PickerFilterType.Video -> selectionVideo
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        cursor ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            loadWithFlow(cursor).collect {
                imagesAdapter.addNewData(it)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        imagesAdapter.setData(emptyList())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(STATE_SELECTION, selectedMediaPaths.toTypedArray())
    }

    private fun checkSelectedItems(mediaItem: MediaItem): Boolean {
        val realPath = mediaItem.media.realPath
        var contains = selectedMediaPaths.contains(realPath)

        if (!contains && requestedSelectionMediaPaths.contains(realPath)) {
            contains = true
            selectedMediaPaths.add(realPath)
        }

        if (contains) selectedMedia.add(mediaItem.media)
        return contains
    }

    private fun loadWithFlow(cursor: Cursor) = callbackFlow<List<MediaItem>> {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val items = ArrayList<MediaItem>()
                val wrongImages = ArrayList<MediaItem>()
                val columnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val columnMediaTypeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val columnDataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(columnIndex)
                    val type = cursor.getInt(columnMediaTypeIndex)
                    var isImage: Boolean
                    var videoDuration = 0.0

                    val contentUri: Uri = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        isImage = false

                        val durationIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                        videoDuration = cursor.getDouble(durationIndex)
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    } else {
                        isImage = true
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }

                    val realPath = cursor.getString(columnDataIndex)
                    val isWrong = !File(realPath).exists()

                    val mediaType = if (isImage) MediaType.Image else MediaType.Video
                    val model = MediaData(contentUri, realPath, isWrong, mediaType = mediaType)
                    val mediaItem = if (isImage) MediaItem.Image(model) else MediaItem.Video(model, videoDuration)
                    mediaItem.media.selected = checkSelectedItems(mediaItem)

                    if (isWrong)
                        wrongImages.add(mediaItem)
                    else
                        items.add(mediaItem)

                    if (items.size == CHUNK_SIZE) {
                        trySend(ArrayList(items))
                        items.clear()
                    }
                }
                cursor.moveToPosition(-1)

                val data = items + wrongImages
                if (data.isNotEmpty())
                    trySend(data)

            } catch (ex: Exception) {
                SceytLog.e(this@BottomSheetMediaPicker.TAG, ex.message.toString())
            } finally {
                withContext(Dispatchers.Main) { setCounter() }
                channel.close()
            }
        }
        awaitClose()
    }

    data class SelectedMediaData(
            val contentUri: Uri,
            val realPath: String,
            val mediaType: MediaType
    )

    enum class MediaType {
        Image,
        Video
    }

    fun interface PickerListener {
        fun onSelect(items: List<SelectedMediaData>)
    }

    override fun getTheme(): Int {
        return R.style.SceytAppBottomSheetDialogTheme
    }

    private fun SceytBottomSheetMediaPickerBinding.applyStyle() {
        tvTitle.text = style.titleText
        style.titleTextStyle.apply(tvTitle)
        style.confirmButtonStyle.apply(btnNext)
        style.countTextStyle.apply(counter)
        style.countBackgroundStyle.apply(counter)
    }

    companion object {
        private const val LOADER_ID = 0x1337
        private const val CHUNK_SIZE = 150
        private const val STATE_SELECTION = "stateSelection"
        private const val MAX_SELECTION_COUNT = "maxSelectionCount"
        private const val FILTER_TYPE = "filterType"
        const val MAX_SELECT_MEDIA_COUNT = 20

        var pickerListener: PickerListener? = null

        fun instance(
                maxSelectCount: Int = MAX_SELECT_MEDIA_COUNT,
                fileFilter: PickerFilterType = PickerFilterType.All,
                vararg selections: String
        ): BottomSheetMediaPicker {
            return BottomSheetMediaPicker().apply {
                arguments = bundleOf(
                    STATE_SELECTION to selections,
                    FILTER_TYPE to fileFilter.ordinal,
                    MAX_SELECTION_COUNT to maxSelectCount)
            }
        }
    }

    enum class PickerFilterType {
        All,
        Image,
        Video
    }
}