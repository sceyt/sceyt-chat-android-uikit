package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.databinding.FragmentChannelLinksBinding
import com.sceyt.chat.ui.extensions.getString
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.extensions.screenHeightPx
import com.sceyt.chat.ui.extensions.setBundleArguments
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.adapters.LinkItem
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.adapters.LinksAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.adapters.viewholders.ChannelLinkViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.viewmodels.ChannelLinksViewModelFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.viewmodels.LinksViewModel
import com.sceyt.chat.ui.shared.helpers.LinkPreviewHelper
import kotlinx.coroutines.launch

open class ChannelLinksFragment : Fragment() {
    private lateinit var channel: SceytChannel
    private var binding: FragmentChannelLinksBinding? = null
    private var linksAdapter: LinksAdapter? = null
    private var pageStateView: PageStateView? = null
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
        loadInitialLinksList()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.getParcelable(CHANNEL))
    }

    private fun initViewModel() {
        lifecycleScope.launch {
            viewModel.messagesFlow.collect(::onInitialLinksList)
        }

        lifecycleScope.launch {
            viewModel.loadMoreMessagesFlow.collect(::onMoreLinksList)
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner, ::onPageStateChange)
    }

    private fun onLinkClick(messageListItem: LinkItem.Link) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(messageListItem.message.body)))
        } catch (ex: Exception) {
        }
    }

    private fun addPageStateView() {
        binding?.root?.addView(PageStateView(requireContext()).apply {
            setEmptyStateView(R.layout.sceyt_empty_state).also {
                it.findViewById<TextView>(R.id.empty_state_title).text = getString(R.string.sceyt_no_link_items_yet)
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

    open fun onInitialLinksList(list: List<LinkItem>) {
        linksAdapter = LinksAdapter(list as ArrayList<LinkItem>,
            ChannelLinkViewHolderFactory(
                requireContext(),
                LinkPreviewHelper(lifecycleScope),
                ::onLinkClick))

        with((binding ?: return).rvLinks) {
            adapter = linksAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isLastItemDisplaying() && viewModel.loadingItems.not() && viewModel.hasNext) {
                        loadMoreLinksList(linksAdapter?.getLastMediaItem()?.message?.id ?: 0)
                    }
                }
            })
        }
    }

    open fun onMoreLinksList(list: List<LinkItem>) {
        linksAdapter?.addNewItems(list)
    }

    open fun onPageStateChange(pageState: PageState) {
        if (pageStateView != null)
            pageStateView?.updateState(pageState, linksAdapter?.itemCount == 0)
    }

    protected fun loadInitialLinksList() {
        viewModel.loadMessages(0, false, mediaType)
    }

    protected fun loadMoreLinksList(lasMsgId: Long) {
        viewModel.loadMessages(lasMsgId, true, mediaType)
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelLinksFragment {
            val fragment = ChannelLinksFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}
