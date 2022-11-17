package com.sceyt.sceytchatuikit.imagepicker

import android.Manifest
import android.content.ContentUris
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.sceytchatuikit.BR
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytBottomSheetGaleryPickerBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.imagepicker.adapter.GalleryMediaAdapter
import com.sceyt.sceytchatuikit.imagepicker.adapter.MediaItem
import kotlinx.parcelize.Parcelize
import java.io.File


class BottomSheetGalleryMediaPicker : BottomSheetDialogFragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private lateinit var binding: SceytBottomSheetGaleryPickerBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private val selectedMedia = mutableSetOf<MediaModel>()
    private var selectedMediaPaths = mutableSetOf<String>()
    private val screenHeight by lazy { screenHeightPx() }
    private val peekHeight by lazy { screenHeight / 1.5 }
    private val imagesAdapter by lazy {
        GalleryMediaAdapter(::onMediaClick)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadArguments()
        if (requireContext().hasPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
        } else {
            requireContext().checkAndAskPermissions(initPermissionLauncher {
                if (it) {
                    LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
                }
            }, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        savedInstanceState?.getStringArray(STATE_SELECTION)?.let {
            selectedMediaPaths = it.toMutableSet()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytBottomSheetGaleryPickerBinding.inflate(inflater, container, false)
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

    private fun SceytBottomSheetGaleryPickerBinding.initViews() {
        btnNext.setOnClickListener {
            pickerListener?.onSelect(selectedMedia.map {
                SelectedMediaData(it.contentUri, it.realPath)
            })
            dismissSafety()
        }
    }

    private fun SceytBottomSheetGaleryPickerBinding.initStyle() {
        val color = requireContext().getCompatColor(pickerStyle.nextButtonColor)
        btnNext.backgroundTintList = ColorStateList.valueOf(color)
        counter.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(30f, 30f, 30f, 30f, 30f, 30f, 30f, 30f)
            setColor(requireContext().getCompatColor(pickerStyle.counterColor))
            setStroke(5, Color.WHITE)
        }
    }

    private fun loadArguments() {
        val args = arguments ?: return
        pickerStyle = args.getParcelable(KEY_MEDIA_PICKER_STYLE) ?: pickerStyle
    }

    private fun onMediaClick(mediaItem: MediaItem) {
        val item = mediaItem.media
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

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        data ?: return
        val items = ArrayList<MediaItem>()

        val columnIndex = data.getColumnIndex(MediaStore.Files.FileColumns._ID)
        val columnMediaTypeIndex = data.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val columnDataIndex = data.getColumnIndex(MediaStore.Files.FileColumns.DATA)

        while (data.moveToNext()) {
            val id = data.getLong(columnIndex)
            val type = data.getInt(columnMediaTypeIndex)
            var isImage: Boolean
            var videoDuration = 0.0

            val contentUri: Uri = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                isImage = false

                val durationIndex = data.getColumnIndex(MediaStore.Video.Media.DURATION)
                videoDuration = data.getDouble(durationIndex)
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

            } else {
                isImage = true
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }

            val realPath = data.getString(columnDataIndex)
            val isWrongImage = !File(realPath).exists()

            val model = MediaModel(contentUri, realPath, isWrongImage)
            val mediaItem = if (isImage) MediaItem.Image(model) else MediaItem.Video(model, videoDuration)
            mediaItem.media.selected = checkSelectedItems(mediaItem)
            items.add(if (isImage) MediaItem.Image(model) else MediaItem.Video(model, videoDuration))
        }
        data.moveToFirst()
        imagesAdapter.submitList(items.sortedBy { it.media.isWrongImage })
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        imagesAdapter.submitList(emptyList())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(STATE_SELECTION, selectedMediaPaths.toTypedArray())
    }

    private fun checkSelectedItems(mediaItem: MediaItem): Boolean {
        val contains = selectedMediaPaths.contains(mediaItem.media.realPath)
        if (contains) selectedMedia.add(mediaItem.media)
        return contains
    }

    data class MediaModel(val contentUri: Uri,
                          val realPath: String,
                          val isWrongImage: Boolean) : BaseObservable() {

        var selected: Boolean = false
            @Bindable get
            set(value) {
                field = value
                notifyPropertyChanged(BR.selected)
            }
    }

    data class SelectedMediaData(val contentUri: Uri,
                                 val realPath: String)

    @Parcelize
    data class MediaPickerStyle(
            @ColorRes
            val nextButtonColor: Int = R.color.sceyt_color_accent,
            @ColorRes
            val counterColor: Int = R.color.sceyt_color_accent,
            @DrawableRes
            val checkedStateIcon: Int = R.drawable.ic_gallery_checked_state,
            @DrawableRes
            val unCheckedStateIcon: Int = R.drawable.ic_gallery_unchecked_state,
    ) : Parcelable

    fun interface PickerListener {
        fun onSelect(items: List<SelectedMediaData>)
    }

    override fun getTheme(): Int {
        return R.style.SceytAppBottomSheetDialogTheme
    }

    companion object {
        private const val LOADER_ID = 0x1337
        private const val STATE_SELECTION = "stateSelection"
        private const val KEY_MEDIA_PICKER_STYLE = "mediaPickerStyle"
        var pickerStyle: MediaPickerStyle = MediaPickerStyle()

        fun createInstance(style: MediaPickerStyle = pickerStyle): BottomSheetGalleryMediaPicker {
            return BottomSheetGalleryMediaPicker().apply {
                arguments = bundleOf(KEY_MEDIA_PICKER_STYLE to style)
            }
        }

        var pickerListener: PickerListener? = null
    }
}