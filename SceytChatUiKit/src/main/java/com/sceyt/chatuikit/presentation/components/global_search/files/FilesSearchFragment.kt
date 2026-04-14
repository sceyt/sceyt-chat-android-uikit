package com.sceyt.chatuikit.presentation.components.global_search.files

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
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.databinding.SceytFragmentFilesSearchBinding
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.persistence.extensions.collectWithLifecycle
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchActivity
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchClickListener
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSession
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionResolver
import com.sceyt.chatuikit.presentation.components.global_search.files.adapter.FilesSearchListAdapter
import com.sceyt.chatuikit.presentation.components.global_search.files.adapter.FilesSearchViewHolderFactory
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.extensions.search.setPageStatesView
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class FilesSearchFragment : Fragment(R.layout.sceyt_fragment_files_search) {
    protected lateinit var session: GlobalSearchSession
    protected lateinit var style: GlobalSearchStyle
    protected open val viewModel: FilesSearchViewModel by viewModels {
        createViewModelFactory(session)
    }

    private var _binding: SceytFragmentFilesSearchBinding? = null
    protected val binding: SceytFragmentFilesSearchBinding
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
        _binding = SceytFragmentFilesSearchBinding.bind(view)
        applyStyle()
        setupRecyclerView()
        observeState()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    protected open fun createViewModelFactory(session: GlobalSearchSession): ViewModelProvider.Factory {
        return FilesSearchViewModelFactory(session)
    }

    protected open fun createListFactory(): FilesSearchViewHolderFactory {
        return FilesSearchViewHolderFactory(
            context = requireContext(),
            style = style.filesPageStyle,
            needMediaDataCallback = viewModel::onNeedMediaInfo,
            onAttachmentClick = { item -> onAttachmentClicked(item.result) },
        )
    }

    protected open fun createListAdapter(): FilesSearchListAdapter {
        return FilesSearchListAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            viewHolderFactory = createListFactory(),
        )
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
            addRVScrollListener(onScrolled = { rv, _, dy ->
                if (dy <= 0 || !shouldLoadMore(rv)) return@addRVScrollListener
                viewModel.loadMore()
            })
        }
    }

    protected open fun observeState() {
        viewModel.state.collectWithLifecycle(
            owner = viewLifecycleOwner,
            collector = ::render,
        )
    }

    protected open fun render(state: FilesSearchState) {
        val settled = state.settled
        val lastKey = viewModel.lastResultsRequestKeyForUI
        val scrollToTop = settled && state.sessionState != lastKey && lastKey != null
        if (settled) viewModel.lastResultsRequestKeyForUI = state.sessionState

        listAdapter.submitList(
            items = state.items,
            commitCallback = if (scrollToTop) {
                { binding.recyclerView.scrollToPosition(0) }
            } else null,
        )

        binding.pageStateView.updateState(
            state = state.toPageState(),
            showLoadingIfNeed = listAdapter.currentList.isEmpty(),
        )
    }

    protected open fun shouldLoadMore(recyclerView: RecyclerView): Boolean {
        val lm = recyclerView.layoutManager as? LinearLayoutManager
            ?: return !recyclerView.canScrollVertically(1)
        val totalCount = lm.itemCount
        if (totalCount == 0) return false
        return lm.findLastVisibleItemPosition() >= totalCount - LOAD_MORE_THRESHOLD
    }

    protected open val clickListener: GlobalSearchClickListener?
        get() = requireActivity() as? GlobalSearchClickListener

    protected open fun onAttachmentClicked(result: GlobalSearchAttachmentResult) {
        clickListener?.onAttachmentClicked(result)
    }

    protected open fun applyStyle() {
        binding.root.setBackgroundColor(style.filesPageStyle.backgroundColor)
        binding.pageStateView.setPageStatesView(style.filesPageStyle)
    }

    companion object {
        private const val LOAD_MORE_THRESHOLD = 5

        fun newInstance(
            styleId: String,
            sessionId: String,
        ) = FilesSearchFragment().setBundleArguments {
            putString(GlobalSearchActivity.STYLE_ID_KEY, styleId)
            putString(GlobalSearchActivity.SESSION_ID_KEY, sessionId)
        }
    }
}

private fun FilesSearchState.toPageState(): PageState = when {
    isLoading -> PageState.StateLoading()
    isLoadingMore -> PageState.StateLoadingMore()
    showEmptyState -> PageState.StateEmpty(query = query)
    else -> PageState.Nothing
}
