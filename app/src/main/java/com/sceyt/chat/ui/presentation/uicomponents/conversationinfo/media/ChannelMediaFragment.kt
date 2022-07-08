package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media

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
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.databinding.FragmentChannelMediaBinding
import com.sceyt.chat.ui.extensions.getString
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.extensions.screenHeightPx
import com.sceyt.chat.ui.extensions.setBundleArguments
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.openFile
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.ChannelAttachmentViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.ChannelMediaAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentViewModelFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentsViewModel
import kotlinx.coroutines.launch

open class ChannelMediaFragment : Fragment() {
    private lateinit var channel: SceytChannel
    private var binding: FragmentChannelMediaBinding? = null
    private var mediaAdapter: ChannelMediaAdapter? = null
    private val mediaType = "media"
    private var pageStateView: PageStateView? = null
    private val viewModel: ChannelAttachmentsViewModel by viewModels {
        getBundleArguments()
        ChannelAttachmentViewModelFactory(channel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelMediaBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addPageStateView()
        initViewModel()
        loadInitialMediaList()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.getParcelable(CHANNEL))
    }

    private fun initViewModel() {
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
                (requireActivity() as? ConversationInfoActivity)?.getViewPagerY()?.let {
                    if (it > 0)
                        layoutParams.height = screenHeightPx() - it
                }
            }
        })
    }

    open fun onInitialMediaList(list: List<FileListItem>) {
        mediaAdapter = ChannelMediaAdapter(list as ArrayList<FileListItem>, ChannelAttachmentViewHolderFactory(requireContext()).also {
            it.setClickListener(AttachmentClickListeners.AttachmentClickListener { _, item ->
                item.openFile(requireContext())
            })
        })
        with((binding ?: return).rvFiles) {
            adapter = mediaAdapter
            layoutManager = GridLayoutManager(requireContext(), 3).also {
                it.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (mediaAdapter?.getItemViewType(position)) {
                            ChannelAttachmentViewHolderFactory.ItemType.Loading.ordinal -> 3
                            else -> 1
                        }
                    }
                }
            }

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isLastItemDisplaying() && viewModel.loadingItems.not() && viewModel.hasNext)
                        loadMoreMediaList(mediaAdapter?.getLastMediaItem()?.sceytMessage?.id ?: 0)
                }
            })
        }
    }

    open fun onMoreMediaList(list: List<FileListItem>) {
        mediaAdapter?.addNewItems(list)
    }

    open fun onPageStateChange(pageState: PageState) {
        if (pageStateView != null)
            pageStateView?.updateState(pageState, mediaAdapter?.itemCount == 0)
    }

    protected fun loadInitialMediaList() {
        viewModel.loadMessages(0, false, mediaType)
    }

    protected fun loadMoreMediaList(lasMsgId: Long) {
        viewModel.loadMessages(lasMsgId, true, mediaType)
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