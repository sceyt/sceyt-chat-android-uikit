package com.sceyt.sceytchatuikit.imagepicker

import android.Manifest
import android.content.ContentUris
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.sceytchatuikit.BR
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytGaleryMediaPickerBinding
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.checkAndAskPermissions
import com.sceyt.sceytchatuikit.extensions.dismissSafety
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getOrientation
import com.sceyt.sceytchatuikit.extensions.initPermissionLauncher
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.screenHeightPx
import com.sceyt.sceytchatuikit.imagepicker.adapter.GalleryMediaAdapter
import com.sceyt.sceytchatuikit.imagepicker.adapter.MediaItem
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.sceytconfigs.GalleryPickerStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class GalleryMediaPicker : BottomSheetDialogFragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private lateinit var binding: SceytGaleryMediaPickerBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private val selectedMedia = mutableSetOf<MediaModel>()
    private var selectedMediaPaths = mutableSetOf<String>()
    private var requestedSelectionMediaPaths = mutableSetOf<String>()
    private val screenHeight by lazy { screenHeightPx() }
    private val peekHeight by lazy { screenHeight / 1.5 }
    private var maxSelectCount: Int = GalleryPickerStyle.maxSelectCount
    private val imagesAdapter by lazy {
        GalleryMediaAdapter(::onMediaClick)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions {
            if (it) LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
        }

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
    }

    private fun checkPermissions(callBack: (Boolean) -> Unit) {
        val resultLauncher = initPermissionLauncher {
            if (it) callBack.invoke(true)
        }

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        val hasAccess = requireContext().checkAndAskPermissions(resultLauncher, *permissions)

        if (hasAccess)
            callBack.invoke(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytGaleryMediaPickerBinding.inflate(inflater, container, false)
            .also {
                binding = it
                binding.initStyle()
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

    private fun SceytGaleryMediaPickerBinding.initViews() {
        btnNext.setOnClickListener {
            pickerListener?.onSelect(selectedMedia.map {
                SelectedMediaData(it.contentUri, it.realPath)
            })
            pickerListener = null
            dismissSafety()
        }
    }

    private fun SceytGaleryMediaPickerBinding.initStyle() {
        val color = requireContext().getCompatColor(GalleryPickerStyle.nextButtonColor)
        btnNext.backgroundTintList = ColorStateList.valueOf(color)
        counter.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(30f, 30f, 30f, 30f, 30f, 30f, 30f, 30f)
            setColor(requireContext().getCompatColor(GalleryPickerStyle.counterColor))
            setStroke(5, Color.WHITE)
        }
    }

    private fun onMediaClick(mediaItem: MediaItem) {
        val item = mediaItem.media
        if (selectedMedia.size == maxSelectCount && item.selected.not()) {
            Toast.makeText(requireContext(), "${context?.getString(R.string.sceyt_max_select_count_should_be)} " +
                    "$maxSelectCount", Toast.LENGTH_SHORT).show()
            return
        }
        item.selected = !item.selected

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

        val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

        val queryUri = MediaStore.Files.getContentUri("external")

        return CursorLoader(requireContext(), queryUri, projection, selection, null, sortOrder)
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

                    val model = MediaModel(contentUri, realPath, isWrong)
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
                SceytLog.e(this@GalleryMediaPicker.TAG, ex.message.toString())
            } finally {
                withContext(Dispatchers.Main) { setCounter() }
                channel.close()
            }
        }
        awaitClose()
    }

    data class MediaModel(val contentUri: Uri,
                          val realPath: String,
                          val isWrong: Boolean) : BaseObservable() {

        var selected: Boolean = false
            @Bindable get
            set(value) {
                field = value
                notifyPropertyChanged(BR.selected)
            }
    }

    data class SelectedMediaData(val contentUri: Uri,
                                 val realPath: String)

    fun interface PickerListener {
        fun onSelect(items: List<SelectedMediaData>)
    }

    override fun getTheme(): Int {
        return R.style.SceytAppBottomSheetDialogTheme
    }

    companion object {
        private const val LOADER_ID = 0x1337
        private const val CHUNK_SIZE = 150
        private const val STATE_SELECTION = "stateSelection"
        private const val MAX_SELECTION_COUNT = "maxSelectionCount"

        var pickerListener: PickerListener? = null

        fun instance(maxSelectCount: Int = GalleryPickerStyle.maxSelectCount, vararg selections: String): GalleryMediaPicker {
            return GalleryMediaPicker().apply {
                arguments = bundleOf(
                    STATE_SELECTION to selections,
                    MAX_SELECTION_COUNT to maxSelectCount)
            }
        }
    }
}