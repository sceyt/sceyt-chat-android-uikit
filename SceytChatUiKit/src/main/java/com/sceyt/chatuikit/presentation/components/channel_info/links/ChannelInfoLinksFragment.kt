package com.sceyt.chatuikit.presentation.components.channel_info.links

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInfoLinksBinding
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.openLink
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.screenHeightPx
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.common.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.components.channel_info.ViewPagerAdapter.HistoryClearedListener
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.ChannelAttachmentViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.ChannelMediaAdapter
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.MediaStickHeaderItemDecoration
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.components.channel_info.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.presentation.di.ChannelInfoLinksViewModelQualifier
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoStyle
import com.sceyt.chatuikit.styles.extensions.channel_info.link.setPageStatesView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

open class ChannelInfoLinksFragment : Fragment, SceytKoinComponent, HistoryClearedListener {
    constructor() : super()

    constructor(infoStyle: ChannelInfoStyle) : super() {
        this.infoStyle = infoStyle
    }

    protected lateinit var channel: SceytChannel
    protected var binding: SceytFragmentChannelInfoLinksBinding? = null
    protected open var mediaAdapter: ChannelMediaAdapter? = null
    protected open var pageStateView: PageStateView? = null
    protected open val mediaType = listOf("link")
    protected val viewModel: ChannelAttachmentsViewModel by viewModel(ChannelInfoLinksViewModelQualifier)
    lateinit var infoStyle: ChannelInfoStyle
        protected set

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
        return SceytFragmentChannelInfoLinksBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViewModel()
        addPageStateView()
        loadInitialLinksList()
        binding?.applyStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(CHANNEL))
    }

    private fun initViewModel() {
        lifecycleScope.launch {
            viewModel.filesFlow.collect(::onInitialLinksList)
        }

        lifecycleScope.launch {
            viewModel.loadMoreFilesFlow.collect(::onMoreLinksList)
        }

        viewModel.linkPreviewLiveData.observe(viewLifecycleOwner, ::onLinkPreview)

        viewModel.pageStateLiveData.observe(viewLifecycleOwner, ::onPageStateChange)
    }

    private fun onLinkClick(url: String?) {
        requireContext().openLink(url)
    }

    private fun addPageStateView() {
        binding?.root?.addView(PageStateView(requireContext()).apply {
            setPageStatesView(this)
            pageStateView = this

            post {
                (activity as? ChannelInfoActivity)?.getViewPagerY()?.let {
                    if (it > 0)
                        layoutParams.height = screenHeightPx() - it
                }
            }
        })
    }

    protected open fun onInitialLinksList(list: List<ChannelFileItem>) {
        if (mediaAdapter == null) {
            val adapter = ChannelMediaAdapter(SyncArrayList(list), ChannelAttachmentViewHolderFactory(
                requireContext(), infoStyle, infoStyle.linkStyle.dateSeparatorStyle
            ).also {
                it.setNeedMediaDataCallback { data -> viewModel.needMediaInfo(data) }

                it.setClickListener(AttachmentClickListeners.AttachmentClickListener { _, item ->
                    onLinkClick(item.attachment.url)
                })
            }).also { mediaAdapter = it }

            with((binding ?: return).rvLinks) {
                this.adapter = adapter
                addItemDecoration(MediaStickHeaderItemDecoration(adapter))

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (isLastItemDisplaying() && viewModel.canLoadPrev()) {
                            loadMoreLinksList(adapter.getLastMediaItem()?.attachment?.id ?: 0,
                                adapter.getFileItems().size)
                        }
                    }
                })
            }
        } else binding?.rvLinks?.let { mediaAdapter?.notifyUpdate(list, it) }
    }

    protected open fun onMoreLinksList(list: List<ChannelFileItem>) {
        mediaAdapter?.addNewItems(list)
    }

    protected open fun onLinkPreview(previewDetails: LinkPreviewDetails) {
        val data = mediaAdapter?.getData() ?: return
        data.findIndexed { it.isMediaItem() && it.attachment.url == previewDetails.link }?.let { (index, item) ->
            item.updateAttachment(item.attachment.copy(linkPreviewDetails = previewDetails))
            mediaAdapter?.updateItemAt(index, item)
        }
    }

    protected open fun onPageStateChange(pageState: PageState) {
        pageStateView?.updateState(
            state = pageState,
            showLoadingIfNeed = (mediaAdapter?.itemCount ?: 0) == 0,
            enableErrorSnackBar = false
        )
    }

    protected open fun loadInitialLinksList() {
        if (channel.pending) {
            binding?.root?.post { pageStateView?.updateState(PageState.StateEmpty()) }
            return
        }
        viewModel.loadAttachments(channel.id, 0, false, mediaType, 0)
    }

    protected fun loadMoreLinksList(lastAttachmentId: Long, offset: Int) {
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

    private fun SceytFragmentChannelInfoLinksBinding.applyStyle() {
        root.setBackgroundColor(infoStyle.linkStyle.backgroundColor)
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(
                channel: SceytChannel,
                infoStyle: ChannelInfoStyle
        ) = ChannelInfoLinksFragment(infoStyle).apply {
            setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
        }
    }
}
