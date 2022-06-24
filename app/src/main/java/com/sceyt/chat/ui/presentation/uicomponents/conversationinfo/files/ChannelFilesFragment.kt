package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.files

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.databinding.FragmentChannelFilesBinding
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.extensions.setBundleArguments
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.ChannelAttachmentViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.ChannelMediaAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentViewModelFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentsViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChannelFilesFragment : Fragment() {
    private lateinit var binding: FragmentChannelFilesBinding
    private lateinit var channel: SceytChannel
    private lateinit var mediaAdapter: ChannelMediaAdapter
    private val mediaType = "file"
    private val viewModel: ChannelAttachmentsViewModel by viewModels {
        getBundleArguments()
        ChannelAttachmentViewModelFactory(channel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelFilesBinding.inflate(inflater, container, false).also {
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

        fun newInstance(channel: SceytChannel): ChannelFilesFragment {
            val fragment = ChannelFilesFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}