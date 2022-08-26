package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.SceytKoinComponent
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters.LinkItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters.LinksAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters.viewholders.ChannelLinkViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.viewmodels.LinksViewModel
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.FragmentChannelLinksBinding
import com.sceyt.sceytchatuikit.extensions.isLastItemDisplaying
import com.sceyt.sceytchatuikit.extensions.openLink
import com.sceyt.sceytchatuikit.extensions.screenHeightPx
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.root.PageStateView
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

open class ChannelLinksFragment : Fragment(), SceytKoinComponent {
    private lateinit var channel: SceytChannel
    private var binding: FragmentChannelLinksBinding? = null
    private var linksAdapter: LinksAdapter? = null
    private var pageStateView: PageStateView? = null
    private val mediaType = "link"
    private val viewModel by viewModel<LinksViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelLinksBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
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
        requireContext().openLink(messageListItem.message.body)
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
                    if (isLastItemDisplaying() && viewModel.loadingItems.get().not() && viewModel.hasNext) {
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
        pageStateView?.updateState(pageState, linksAdapter?.itemCount == 0)
    }

    protected fun loadInitialLinksList() {
        viewModel.loadMessages(channel, 0, false, mediaType)
    }

    protected fun loadMoreLinksList(lasMsgId: Long) {
        viewModel.loadMessages(channel, lasMsgId, true, mediaType)
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
