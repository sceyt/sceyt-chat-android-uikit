package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.databinding.FragmentChannelLinksBinding
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.extensions.setBundleArguments
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.adapters.viewholders.ChannelLinkViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.adapters.LinkItem
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.adapters.LinksAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.viewmodels.ChannelLinksViewModelFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.viewmodels.LinksViewModel
import com.sceyt.chat.ui.shared.helpers.LinkPreviewHelper
import kotlinx.coroutines.launch

class ChannelLinksFragment : Fragment() {
    private lateinit var binding: FragmentChannelLinksBinding
    private lateinit var channel: SceytChannel
    private lateinit var linksAdapter: LinksAdapter
    private lateinit var pageStateView: PageStateView
    private val mediaType = "link"
    private val viewModel: LinksViewModel by viewModels {
        getBundleArguments()
        ChannelLinksViewModelFactory(channel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelLinksBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addPageStateView()
        initViewModel()
        viewModel.loadMessages(0, false, mediaType)
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.getParcelable(CHANNEL))
    }

    private fun initViewModel() {
        lifecycleScope.launch {
            viewModel.messagesFlow.collect {
                setupList(it)
            }
        }

        lifecycleScope.launch {
            viewModel.loadMoreMessagesFlow.collect {
                linksAdapter.addNewItems(it)
            }
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner) {
            if (::pageStateView.isInitialized)
                pageStateView.updateState(it, linksAdapter.itemCount == 0)
        }
    }

    private fun setupList(list: List<LinkItem>) {
        linksAdapter = LinksAdapter(list as ArrayList<LinkItem>,
            ChannelLinkViewHolderFactory(
                requireContext(),
                LinkPreviewHelper(lifecycleScope),
                ::onLinkClick))

        with(binding.rvLinks) {
            adapter = linksAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isLastItemDisplaying() && viewModel.loadingItems.not() && viewModel.hasNext) {
                        viewModel.loadMessages(linksAdapter.getLastMediaItem()?.message?.id
                                ?: 0, true, mediaType)
                    }
                }
            })
        }
    }

    private fun onLinkClick(messageListItem: LinkItem.Link) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(messageListItem.message.body)))
        } catch (ex: Exception) {
        }
    }

    private fun addPageStateView() {
        binding.root.addView(PageStateView(requireContext()).apply {
            setEmptyStateView(R.layout.sceyt_files_list_empty_state)
            setLoadingStateView(R.layout.sceyt_loading_state)
            pageStateView = this
        })
    }

    companion object {
        private const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelLinksFragment {
            val fragment = ChannelLinksFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}
