package com.sceyt.chatuikit.presentation.components.picker

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.databinding.SceytBottomSheetMediaPickerBinding
import com.sceyt.chatuikit.extensions.dismissSafety
import com.sceyt.chatuikit.extensions.getOrientation
import com.sceyt.chatuikit.extensions.hasOneOfPermissions
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.parcelableArrayList
import com.sceyt.chatuikit.extensions.screenHeightPx
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.persistence.differs.GalleryMediaItemDiff
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaItem
import com.sceyt.chatuikit.presentation.components.picker.adapter.holders.MediaPickerItemViewHolderFactory
import com.sceyt.chatuikit.styles.extensions.common.applyStyle
import com.sceyt.chatuikit.styles.media_picker.MediaPickerStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize


class BottomSheetMediaPicker : BottomSheetDialogFragment() {
    private lateinit var binding: SceytBottomSheetMediaPickerBinding
    private val viewModel: MediaPickerViewModel by viewModels {
        createViewModelFactory()
    }
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private val screenHeight by lazy { screenHeightPx() }
    private val peekHeight by lazy { screenHeight / 1.5 }
    private var filterType: PickerFilterType = PickerFilterType.All
    private lateinit var style: MediaPickerStyle
    private val imagesAdapter by lazy {
        MediaAdapter(initGalleryViewHolderFactory())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getInt(FILTER_TYPE)?.let {
            filterType = PickerFilterType.entries[it]
        }

        checkPermissions { granted ->
            if (granted)
                viewModel.loadMedia()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refreshIfGalleryChanged()
    }

    private fun createViewModelFactory(): ViewModelProvider.Factory {
        val maxSelectCount = arguments?.getInt(MAX_SELECTION_COUNT) ?: MAX_SELECT_MEDIA_COUNT
        val preselectedPaths = arguments?.getStringArray(STATE_SELECTION)
            ?.filter { path -> path.isNotNullOrBlank() }
            .orEmpty()

        return MediaPickerViewModelFactory(
            contentResolver = requireContext().applicationContext.contentResolver,
            filterType = filterType,
            maxSelectCount = maxSelectCount,
            preselectedPaths = preselectedPaths,
        )
    }

    private fun initGalleryViewHolderFactory(): MediaPickerItemViewHolderFactory {
        val clickListener = MediaAdapter.MediaClickListener(::onMediaClick)
        return MediaPickerItemViewHolderFactory(style.itemStyle, clickListener)
    }

    private fun checkPermissions(callBack: (granted: Boolean) -> Unit) {
        val permissions = filterType.readPermissions()
        val resultLauncher = registerForActivityResult(RequestMultiplePermissions()) {
            callBack(requireContext().hasOneOfPermissions(*permissions))
        }
        if (requireContext().hasOneOfPermissions(*permissions)) {
            callBack(true)
        } else {
            resultLauncher.launch(permissions)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
            val spanCount =
                if (requireContext().getOrientation() == Configuration.ORIENTATION_LANDSCAPE)
                    5 else 3

            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), spanCount)
            adapter = imagesAdapter
        }

        binding.initViews()
        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state
                .map { it.items }
                .distinctUntilChanged()
                .collect { items ->
                    imagesAdapter.setData(items)
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                setCounter(state.selectedCount)
                binding.layoutEmptyState.root.isVisible = state.isLoaded && state.items.isEmpty()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet =
                    findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
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
            val result = ArrayList(viewModel.getSelectedMedia())
            setFragmentResult(REQUEST_KEY, Bundle().apply {
                putParcelableArrayList(RESULT_SELECTED_MEDIA, result)
            })
            dismissSafety()
        }
    }

    private fun onMediaClick(mediaItem: MediaItem, position: Int) {
        if (position == RecyclerView.NO_POSITION) return

        if (!viewModel.toggleSelection(mediaItem.media)) {
            Toast.makeText(
                requireContext(), "${context?.getString(R.string.sceyt_you_can_select_max)} " +
                        "${viewModel.maxSelectCount}", Toast.LENGTH_SHORT
            ).show()
            return
        }
        imagesAdapter.notifyItemChanged(
            position,
            GalleryMediaItemDiff.DEFAULT.copy(filePathChanged = false)
        )
    }

    private fun setCounter(count: Int) {
        binding.counter.isVisible = count > 0
        binding.counter.text = count.toString()
    }

    private val bottomSheetCallback by lazy {
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val bottomSheetVisibleHeight = bottomSheet.height - bottomSheet.top
                if (bottomSheetVisibleHeight >= peekHeight)
                    binding.layoutCounter.translationY =
                        -((bottomSheet.height - bottomSheetVisibleHeight).toFloat())
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    dismissAllowingStateLoss()
            }
        }
    }

    @Parcelize
    data class SelectedMediaData(
        val contentUri: Uri,
        val realPath: String,
        val mediaType: MediaType,
    ) : Parcelable

    enum class MediaType(val value: AttachmentTypeEnum) {
        Image(AttachmentTypeEnum.Image),
        Video(AttachmentTypeEnum.Video)
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
        layoutEmptyState.applyStyle(style.emptyStateStyle)
    }

    companion object {
        private const val STATE_SELECTION = "stateSelection"
        private const val MAX_SELECTION_COUNT = "maxSelectionCount"
        private const val FILTER_TYPE = "filterType"
        const val MAX_SELECT_MEDIA_COUNT = 20

        const val REQUEST_KEY = "BottomSheetMediaPicker.REQUEST_KEY"
        private const val RESULT_SELECTED_MEDIA = "RESULT_SELECTED_MEDIA"

        fun getSelectedMedia(result: Bundle): List<SelectedMediaData> =
            result.parcelableArrayList(RESULT_SELECTED_MEDIA) ?: emptyList()

        fun instance(
            maxSelectCount: Int = MAX_SELECT_MEDIA_COUNT,
            fileFilter: PickerFilterType = PickerFilterType.All,
            vararg selections: String,
        ): BottomSheetMediaPicker {
            return BottomSheetMediaPicker().setBundleArguments {
                putStringArray(STATE_SELECTION, selections)
                putInt(FILTER_TYPE, fileFilter.ordinal)
                putInt(MAX_SELECTION_COUNT, maxSelectCount)
            }
        }
    }

    enum class PickerFilterType {
        All,
        Image,
        Video;

        internal fun readPermissions(): Array<String> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            val visualPermissions = when (this) {
                All -> arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )

                Image -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                Video -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                visualPermissions + Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            } else {
                visualPermissions
            }
        }
    }
}