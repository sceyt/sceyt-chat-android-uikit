package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.databinding.FragmentChannelMediaBinding
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.extensions.setBundleArguments
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.ChannelAttachmentViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.ChannelMediaAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentViewModelFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentsViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChannelMediaFragment : Fragment() {
    private lateinit var binding: FragmentChannelMediaBinding
    private lateinit var channel: SceytChannel
    private lateinit var mediaAdapter: ChannelMediaAdapter
    private val mediaType = "media"
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

        initViewModel()
        viewModel.loadMessages(0, false, mediaType)
    }

    private fun getBundleArguments() {
        channel = arguments?.getParcelable(CHANNEL)!!
    }

    private fun initViewModel() {
        lifecycleScope.launch {
            viewModel.filesFlow.collect {
                setupList(it)
            }
        }

        lifecycleScope.launch {
            viewModel.loadMoreFilesFlow.collect {
                mediaAdapter.addNewItems(it)
            }
        }
    }

    private fun setupList(list: List<FileListItem>) {
        mediaAdapter = ChannelMediaAdapter(list as ArrayList<FileListItem>, ChannelAttachmentViewHolderFactory(requireContext()))
        with(binding.rvFiles) {
            adapter = mediaAdapter
            layoutManager = GridLayoutManager(requireContext(), 3).also {
                it.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (mediaAdapter.getItemViewType(position)) {
                            ChannelAttachmentViewHolderFactory.ItemType.Loading.ordinal -> 3
                            else -> 1
                        }
                    }
                }
            }

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isLastItemDisplaying() && viewModel.isLoadingAttachments.not() && viewModel.hasNext) {
                        viewModel.loadMessages(mediaAdapter.getLastMediaItem()?.sceytMessage?.id
                                ?: 0, true, mediaType)
                    }
                }
            })
        }
    }

    companion object {
        private const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelMediaFragment {
            val fragment = ChannelMediaFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}