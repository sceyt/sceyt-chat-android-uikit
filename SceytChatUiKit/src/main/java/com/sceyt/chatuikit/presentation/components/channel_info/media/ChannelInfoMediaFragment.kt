package com.sceyt.chatuikit.presentation.components.channel_info.media

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInfoMediaBinding
import com.sceyt.chatuikit.extensions.isLandscape
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.screenHeightPx
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.common.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem.Companion.getData
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.components.channel_info.ViewPagerAdapter.HistoryClearedListener
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.ChannelAttachmentViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.ChannelMediaAdapter
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.MediaStickHeaderItemDecoration
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.components.channel_info.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewActivity
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.presentation.di.ChannelInfoMediaViewModelQualifier
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoStyle
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

open class ChannelInfoMediaFragment : Fragment, SceytKoinComponent, HistoryClearedListener {
    constructor() : super()

    constructor(infoStyle: ChannelInfoStyle) : super() {
        this.infoStyle = infoStyle
    }

    protected lateinit var channel: SceytChannel
    protected var binding: SceytFragmentChannelInfoMediaBinding? = null
    protected open var mediaAdapter: ChannelMediaAdapter? = null
    protected open val mediaType = listOf("image", "video")
    protected open var pageStateView: PageStateView? = null
    protected val viewModel: ChannelAttachmentsViewModel by viewModel(ChannelInfoMediaViewModelQualifier)
    protected lateinit var infoStyle: ChannelInfoStyle

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Keep the style in the view model.
        // If the style is not initialized it will be taken from the view model.
        if (::infoStyle.isInitialized)
            viewModel.infoStyle = infoStyle
        else
            infoStyle = viewModel.infoStyle
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelInfoMediaBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViewModel()
        addPageStateView()
        loadInitialMediaList()
        binding?.applyStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(CHANNEL))
    }

    private fun initViewModel() {
        viewModel.observeToUpdateAfterOnResume(this)

        lifecycleScope.launch {
            viewModel.filesFlow.collect(::onInitialMediaList)
        }

        lifecycleScope.launch {
            viewModel.loadMoreFilesFlow.collect(::onMoreMediaList)
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner, ::onPageStateChange)
    }

    private fun addPageStateView() {
        binding?.root?.addView(PageStateView(requireContext()).apply {
            setEmptyStateView(R.layout.sceyt_empty_state).also {
                it.findViewById<TextView>(R.id.empty_state_title).text = getString(R.string.sceyt_no_media_items_yet)
            }
            setLoadingStateView(R.layout.sceyt_loading_state)
            pageStateView = this

            post {
                (activity as? ChannelInfoActivity)?.getViewPagerY()?.let {
                    if (it > 0)
                        layoutParams.height = screenHeightPx() - it
                }
            }
        })
    }

    protected open fun onInitialMediaList(list: List<ChannelFileItem>) {
        if (mediaAdapter == null) {
            val adapter = ChannelMediaAdapter(SyncArrayList(list), ChannelAttachmentViewHolderFactory(
                requireContext(), infoStyle, infoStyle.mediaStyle.dateSeparatorStyle).also {

                it.setNeedMediaDataCallback { data ->
                    viewModel.needMediaInfo(data)
                }

                it.setClickListener(AttachmentClickListeners.AttachmentClickListener { _, item ->
                    onMediaClick(item)
                })
            }).also { mediaAdapter = it }

            with((binding ?: return).rvFiles) {
                setHasFixedSize(true)
                this.adapter = adapter
                layoutManager = GridLayoutManager(requireContext(), getSpanCount()).also {
                    it.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return when (adapter.getItemViewType(position)) {
                                ChannelAttachmentViewHolderFactory.ItemType.Loading.ordinal -> getSpanCount()
                                ChannelAttachmentViewHolderFactory.ItemType.MediaDate.ordinal -> getSpanCount()
                                else -> 1
                            }
                        }
                    }
                }

                addItemDecoration(MediaStickHeaderItemDecoration(adapter))

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (isLastItemDisplaying() && viewModel.canLoadPrev())
                            loadMoreMediaList(adapter.getLastMediaItem()?.file?.id
                                    ?: 0, adapter.getFileItems().size)
                    }
                })
            }
        } else binding?.rvFiles?.let { mediaAdapter?.notifyUpdate(list, it) }
    }

    protected open fun getSpanCount(): Int {
        return if (requireContext().isLandscape()) {
            6
        } else 3
    }

    protected open fun onMediaClick(item: ChannelFileItem) {
        item.getData()?.let { data ->
            MediaPreviewActivity.launch(requireContext(), data.attachment, data.user, channel.id, true)
        }
    }

    protected open fun onMoreMediaList(list: List<ChannelFileItem>) {
        mediaAdapter?.addNewItems(list)
    }

    protected open fun onPageStateChange(pageState: PageState) {
        pageStateView?.updateState(pageState, mediaAdapter?.itemCount == 0, enableErrorSnackBar = false)
    }

    protected open fun loadInitialMediaList() {
        if (channel.pending) {
            binding?.root?.post { pageStateView?.updateState(PageState.StateEmpty()) }
            return
        }
        viewModel.loadAttachments(channel.id, 0, false, mediaType, 0)
    }

    protected fun loadMoreMediaList(lastAttachmentId: Long, offset: Int) {
        viewModel.loadAttachments(channel.id, lastAttachmentId, true, mediaType, offset)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaAdapter = null
    }

    override fun onHistoryCleared() {
        mediaAdapter?.clearData()
        pageStateView?.updateState(PageState.StateEmpty())
    }

    private fun SceytFragmentChannelInfoMediaBinding.applyStyle() {
        root.setBackgroundColor(infoStyle.mediaStyle.backgroundColor)
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(
                channel: SceytChannel,
                infoStyle: ChannelInfoStyle
        ) = ChannelInfoMediaFragment(infoStyle).apply {
            setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
        }
    }
}