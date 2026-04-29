package com.sceyt.chatuikit.presentation.components.global_search.media

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.databinding.SceytFragmentMediaSearchBinding
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.persistence.extensions.collectWithLifecycle
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.MediaStickHeaderItemDecoration
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchActivity
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchClickListener
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSession
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionResolver
import com.sceyt.chatuikit.presentation.components.global_search.media.adapter.MediaSearchListAdapter
import com.sceyt.chatuikit.presentation.components.global_search.media.adapter.MediaSearchViewHolderFactory
import com.sceyt.chatuikit.presentation.components.global_search.media.adapter.grid.MediaSearchGridAdapter
import com.sceyt.chatuikit.presentation.components.global_search.media.adapter.grid.MediaSearchGridViewHolderFactory
import com.sceyt.chatuikit.presentation.components.global_search.media.adapter.grid.MediaSearchGridViewHolderFactory.ItemType
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.extensions.search.setPageStatesView
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class MediaSearchFragment : Fragment(R.layout.sceyt_fragment_media_search) {
    protected lateinit var session: GlobalSearchSession
    protected lateinit var style: GlobalSearchStyle
    protected open val viewModel: MediaSearchViewModel by viewModels {
        createViewModelFactory(session)
    }

    private var _binding: SceytFragmentMediaSearchBinding? = null
    protected val binding: SceytFragmentMediaSearchBinding
        get() = checkNotNull(_binding)

    private var isGridMode = false

    private val gridAdapter by lazy {
        MediaSearchGridAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            factory = createGridFactory(),
        )
    }
    private val listAdapter by lazy { createListAdapter() }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val styleId = arguments?.getString(GlobalSearchActivity.STYLE_ID_KEY)
        style = StyleRegistry.getOrDefault(styleId) {
            GlobalSearchStyle.Builder(context).build()
        }
        session = GlobalSearchSessionResolver.resolve(arguments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = SceytFragmentMediaSearchBinding.bind(view)
        applyStyle()
        observeState()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    protected open fun createViewModelFactory(session: GlobalSearchSession): ViewModelProvider.Factory {
        return MediaSearchViewModelFactory(session)
    }

    protected open fun createGridFactory(): MediaSearchGridViewHolderFactory {
        return MediaSearchGridViewHolderFactory(
            context = requireContext(),
            style = style.mediaPageStyle,
            needMediaDataCallback = viewModel::onNeedMediaInfo,
            onItemClick = ::onGridAttachmentClicked,
        )
    }

    protected open fun createListFactory(): MediaSearchViewHolderFactory {
        return MediaSearchViewHolderFactory(
            context = requireContext(),
            style = style.mediaPageStyle,
            needMediaDataCallback = viewModel::onNeedMediaInfo,
            onAttachmentClick = { item -> onAttachmentClicked(item.result) },
        )
    }

    protected open fun createListAdapter(): MediaSearchListAdapter {
        return MediaSearchListAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            viewHolderFactory = createListFactory(),
        )
    }

    protected open fun observeState() {
        viewModel.state.collectWithLifecycle(
            owner = viewLifecycleOwner,
            collector = ::render,
        )
    }

    protected open fun render(state: MediaSearchState) {
        when (val mode = state.mode) {
            is MediaSearchDisplayMode.Grid -> renderGrid(state, mode)
            is MediaSearchDisplayMode.SearchList -> renderList(state, mode)
        }
        clearInactiveAdapter(state.mode)
        binding.pageStateView.updateState(
            state = state.toPageState(),
            showLoadingIfNeed = when (state.mode) {
                is MediaSearchDisplayMode.Grid -> gridAdapter.currentList.isEmpty()
                is MediaSearchDisplayMode.SearchList -> listAdapter.currentList.isEmpty()
            }
        )
    }

    private fun renderGrid(state: MediaSearchState, mode: MediaSearchDisplayMode.Grid) {
        if (!isGridMode) {
            isGridMode = true
            binding.recyclerView.apply {
                while (itemDecorationCount > 0) removeItemDecorationAt(0)
                clearOnScrollListeners()
                layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT).also { lm ->
                    lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return when (gridAdapter.getItemViewType(position)) {
                                ItemType.DateSeparator.ordinal,
                                ItemType.Loading.ordinal -> GRID_SPAN_COUNT

                                else -> 1
                            }
                        }
                    }
                }
                adapter = gridAdapter
                addItemDecoration(MediaStickHeaderItemDecoration(gridAdapter))
                addRVScrollListener(onScrolled = { rv, _, dy ->
                    if (dy <= 0 || !shouldLoadMore(rv)) return@addRVScrollListener
                    viewModel.loadMore()
                })
            }
        }

        val settled = state.settled
        val lastKey = viewModel.lastResultsRequestKeyForUI
        val scrollToTop = settled && state.sessionState != lastKey && lastKey != null
        if (settled) viewModel.onResultsRendered(state.sessionState)

        val gridItems = if (state.isLoadingMore)
            mode.items + GlobalSearchListItem.Loading else mode.items

        gridAdapter.submitList(
            items = gridItems,
            commitCallback = if (scrollToTop) {
                { binding.recyclerView.scrollToPosition(0) }
            } else null,
        )
    }

    private fun renderList(state: MediaSearchState, mode: MediaSearchDisplayMode.SearchList) {
        if (isGridMode) {
            isGridMode = false
            binding.recyclerView.apply {
                while (itemDecorationCount > 0) removeItemDecorationAt(0)
                clearOnScrollListeners()
                layoutManager = LinearLayoutManager(requireContext())
                adapter = listAdapter
            }
        }

        val settled = !state.isLoading && !state.isLoadingMore
        val lastKey = viewModel.lastResultsRequestKeyForUI
        val scrollToTop = settled && state.sessionState != lastKey && lastKey != null
        if (settled) viewModel.onResultsRendered(state.sessionState)

        val listItems = if (state.isLoadingMore)
            mode.items + GlobalSearchListItem.Loading else mode.items

        listAdapter.submitList(
            items = listItems,
            query = state.query,
            commitCallback = if (scrollToTop) {
                { binding.recyclerView.scrollToPosition(0) }
            } else null,
        )
    }

    private fun clearInactiveAdapter(newMode: MediaSearchDisplayMode) {
        when (newMode) {
            is MediaSearchDisplayMode.Grid -> listAdapter.submitList(emptyList(), "")
            is MediaSearchDisplayMode.SearchList -> gridAdapter.submitList(emptyList())
        }
    }

    protected open fun shouldLoadMore(recyclerView: RecyclerView): Boolean {
        val lm = recyclerView.layoutManager as? GridLayoutManager
            ?: return !recyclerView.canScrollVertically(1)
        val totalCount = lm.itemCount
        if (totalCount == 0) return false
        return lm.findLastVisibleItemPosition() >= totalCount - LOAD_MORE_THRESHOLD
    }

    protected open val clickListener: GlobalSearchClickListener?
        get() = requireActivity() as? GlobalSearchClickListener

    protected open fun onGridAttachmentClicked(
        sharedView: View,
        result: GlobalSearchAttachmentResult,
    ) {
        launchMediaPreview(sharedView, result)
    }

    protected open fun launchMediaPreview(
        sharedView: View,
        result: GlobalSearchAttachmentResult,
    ) {
        val allResults = viewModel.state.value.mode.getListItems()
            .filterIsInstance<GlobalSearchListItem.AttachmentItem>()
            .map { it.result }
        clickListener?.onMediaAttachmentClicked(sharedView, result, allResults)
    }

    protected open fun onAttachmentClicked(result: GlobalSearchAttachmentResult) {
        clickListener?.onAttachmentClicked(result)
    }

    protected open fun applyStyle() {
        binding.root.setBackgroundColor(style.mediaPageStyle.backgroundColor)
        binding.pageStateView.setPageStatesView(style.mediaPageStyle)
    }

    companion object {
        private const val GRID_SPAN_COUNT = 3
        private const val LOAD_MORE_THRESHOLD = 9

        fun newInstance(
            styleId: String,
            sessionId: String,
        ) = MediaSearchFragment().setBundleArguments {
            putString(GlobalSearchActivity.STYLE_ID_KEY, styleId)
            putString(GlobalSearchSessionResolver.SESSION_ID_KEY, sessionId)
        }
    }
}

private fun MediaSearchState.toPageState(): PageState = when {
    isLoading -> PageState.StateLoading()
    isLoadingMore -> PageState.StateLoadingMore()
    showEmptyState -> PageState.StateEmpty(query = query)
    else -> PageState.Nothing
}
