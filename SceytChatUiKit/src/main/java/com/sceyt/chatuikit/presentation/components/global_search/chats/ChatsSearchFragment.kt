package com.sceyt.chatuikit.presentation.components.global_search.chats

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentChatsSearchBinding
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.persistence.extensions.collectWithLifecycle
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchActivity
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchClickListener
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSession
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionResolver
import com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.ChatsSearchListAdapter
import com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.ChatsSearchViewHolderFactory
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.extensions.search.setPageStatesView
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class ChatsSearchFragment : Fragment(R.layout.sceyt_fragment_chats_search) {
    protected lateinit var session: GlobalSearchSession
    protected lateinit var style: GlobalSearchStyle
    protected open val viewModel: ChatsSearchViewModel by viewModels {
        createViewModelFactory(session)
    }
    private var _binding: SceytFragmentChatsSearchBinding? = null
    protected val binding: SceytFragmentChatsSearchBinding
        get() = checkNotNull(_binding)

    private val listAdapter by lazy { createListAdapter() }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val styleId = arguments?.getString(GlobalSearchActivity.STYLE_ID_KEY)
        style = StyleRegistry.getOrDefault(styleId) {
            GlobalSearchStyle.Builder(context).build()
        }
        session = GlobalSearchSessionResolver.require(arguments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = SceytFragmentChatsSearchBinding.bind(view)
        initViews()
        applyStyle()
        observeState()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    protected open fun initViews() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = listAdapter
        binding.recyclerView.addRVScrollListener(onScrolled = { recyclerView, _, dy ->
            if (dy <= 0 || !shouldLoadMore(recyclerView)) return@addRVScrollListener
            viewModel.loadMore()
        })
    }

    protected open fun observeState() {
        viewModel.state.collectWithLifecycle(
            owner = viewLifecycleOwner,
            collector = ::render
        )
    }

    protected open fun createViewModelFactory(session: GlobalSearchSession): ViewModelProvider.Factory {
        return ChatsSearchViewModelFactory(session)
    }

    protected open fun createViewHolderFactory(): ChatsSearchViewHolderFactory {
        return ChatsSearchViewHolderFactory(
            context = requireContext(),
            style = style,
            onChannelClick = ::onChannelClicked,
            onMessageClick = ::onMessageClicked
        )
    }

    protected open fun createListAdapter(): ChatsSearchListAdapter {
        return ChatsSearchListAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            viewHolderFactory = createViewHolderFactory()
        )
    }

    protected open fun render(state: ChatsSearchState) {
        val settled = !state.isLoading && !state.isLoadingMore
        val lastResultsRequestKey = viewModel.lastResultsRequestKeyForUI
        val scrollToTop =
            settled && state.sessionState != lastResultsRequestKey && lastResultsRequestKey != null
        if (settled) viewModel.lastResultsRequestKeyForUI = state.sessionState

        listAdapter.submitList(
            items = state.listItems,
            query = state.query,
            commitCallback = if (scrollToTop) {
                { binding.recyclerView.scrollToPosition(0) }
            } else null
        )
        binding.pageStateView.updateState(
            state = state.toPageState(),
            showLoadingIfNeed = listAdapter.currentList.isEmpty()
        )
        requestMoreIfViewportNotFilled(state)
    }


    protected open val clickListener: GlobalSearchClickListener?
        get() = requireActivity() as? GlobalSearchClickListener

    protected open fun onChannelClicked(channel: SceytChannel) {
        clickListener?.onChannelClicked(channel)
    }

    protected open fun onMessageClicked(messageId: Long, channel: SceytChannel) {
        clickListener?.onMessageClicked(messageId, channel)
    }

    protected open fun requestMoreIfViewportNotFilled(state: ChatsSearchState) {
        if (state.sessionState?.query?.isNotBlank() == true || state.isLoading || state.isLoadingMore || !state.hasMore) return

        binding.recyclerView.post {
            if (_binding == null || binding.recyclerView.canScrollVertically(1)) return@post
            viewModel.loadMore()
        }
    }

    protected open fun shouldLoadMore(recyclerView: RecyclerView): Boolean {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            ?: return !recyclerView.canScrollVertically(1)
        val totalCount = layoutManager.itemCount
        if (totalCount == 0) return false
        return layoutManager.findLastVisibleItemPosition() >= totalCount - LOAD_MORE_THRESHOLD
    }

    protected open fun applyStyle() {
        binding.root.setBackgroundColor(style.chatsPageStyle.backgroundColor)
        binding.pageStateView.setPageStatesView(style.chatsPageStyle)
    }

    companion object {
        private const val LOAD_MORE_THRESHOLD = 6

        fun newInstance(
            styleId: String,
            sessionId: String,
        ) = ChatsSearchFragment().setBundleArguments {
            putString(GlobalSearchActivity.STYLE_ID_KEY, styleId)
            putString(GlobalSearchActivity.SESSION_ID_KEY, sessionId)
        }
    }
}

private fun ChatsSearchState.toPageState(): PageState = when {
    isLoading -> PageState.StateLoading()
    isLoadingMore -> PageState.StateLoadingMore()
    showEmptyState -> PageState.StateEmpty(query = query)
    else -> PageState.Nothing
}
