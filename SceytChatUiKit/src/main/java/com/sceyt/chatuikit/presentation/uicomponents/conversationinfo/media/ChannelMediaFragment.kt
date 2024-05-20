package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentChannelMediaBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isLandscape
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.screenHeightPx
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.common.SyncArrayList
import com.sceyt.chatuikit.presentation.customviews.SceytPageStateView
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem.Companion.getData
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.SceytConversationInfoActivity
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ViewPagerAdapter
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.ChannelAttachmentViewHolderFactory
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.ChannelMediaAdapter
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.MediaStickHeaderItemDecoration
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.chatuikit.presentation.uicomponents.mediaview.SceytMediaActivity
import com.sceyt.chatuikit.sceytstyles.ConversationInfoMediaStyle
import kotlinx.coroutines.launch

open class ChannelMediaFragment : Fragment(), SceytKoinComponent, ViewPagerAdapter.HistoryClearedListener {
    protected lateinit var channel: SceytChannel
    protected var binding: SceytFragmentChannelMediaBinding? = null
    protected open var mediaAdapter: ChannelMediaAdapter? = null
    protected open val mediaType = listOf("image", "video")
    protected open var pageStateView: SceytPageStateView? = null
    protected lateinit var viewModel: ChannelAttachmentsViewModel
    protected lateinit var style: ConversationInfoMediaStyle
        private set

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = ConversationInfoMediaStyle.Builder(context, null).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelMediaBinding.inflate(inflater, container, false).also {
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
        viewModel = viewModels<ChannelAttachmentsViewModel>().value

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
        binding?.root?.addView(SceytPageStateView(requireContext()).apply {
            setEmptyStateView(R.layout.sceyt_empty_state).also {
                it.findViewById<TextView>(R.id.empty_state_title).text = getString(R.string.sceyt_no_media_items_yet)
            }
            setLoadingStateView(R.layout.sceyt_loading_state)
            pageStateView = this

            post {
                (activity as? SceytConversationInfoActivity)?.getViewPagerY()?.let {
                    if (it > 0)
                        layoutParams.height = screenHeightPx() - it
                }
            }
        })
    }

    protected open fun onInitialMediaList(list: List<ChannelFileItem>) {
        if (mediaAdapter == null) {
            val adapter = ChannelMediaAdapter(SyncArrayList(list), ChannelAttachmentViewHolderFactory(
                requireContext(), style).also {

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
                                ChannelAttachmentViewHolderFactory.ItemType.Loading.ordinal -> 3
                                ChannelAttachmentViewHolderFactory.ItemType.MediaDate.ordinal -> 3
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
            SceytMediaActivity.openMediaView(requireContext(), data.attachment, data.user, channel.id, true)
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

    private fun SceytFragmentChannelMediaBinding.applyStyle() {
        root.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.backgroundColor))
    }

    companion object {

        const val CHANNEL = "CHANNEL"
        fun newInstance(channel: SceytChannel): ChannelMediaFragment {
            val fragment = ChannelMediaFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}