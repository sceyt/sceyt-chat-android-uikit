package com.sceyt.chatuikit.presentation.components.global_search

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytFragmentGlobalSearchMediaBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.presentation.components.global_search.adapters.GlobalSearchListAdapter
import com.sceyt.chatuikit.presentation.components.global_search.adapters.GlobalSearchMediaGridAdapter
import com.sceyt.chatuikit.shared.helpers.RecyclerItemOffsetDecoration
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle
import kotlinx.coroutines.launch

open class MediaSearchFragment : Fragment(R.layout.sceyt_fragment_global_search_media) {
    companion object {
        private const val LOAD_MORE_THRESHOLD = 6
        
        fun newInstance(
            styleId: String,
            sessionId: String,
        ) = MediaSearchFragment().setBundleArguments {
            putString(GlobalSearchActivity.STYLE_ID_KEY, styleId)
            putString(GlobalSearchActivity.SESSION_ID_KEY, sessionId)
        }
    }

    protected open val hostActivity: GlobalSearchActivity
        get() = requireActivity() as? GlobalSearchActivity
            ?: error("MediaSearchFragment must be hosted by GlobalSearchActivity.")

    protected lateinit var session: GlobalSearchSession
    protected open val viewModel: GlobalSearchTabViewModel by viewModels {
        createViewModelFactory(session)
    }

    protected lateinit var style: GlobalSearchStyle

    private var _binding: SceytFragmentGlobalSearchMediaBinding? = null
    protected val binding: SceytFragmentGlobalSearchMediaBinding
        get() = checkNotNull(_binding)

    private val listAdapter by lazy(LazyThreadSafetyMode.NONE) { createListAdapter() }
    private val mediaAdapter by lazy(LazyThreadSafetyMode.NONE) { createMediaGridAdapter() }

    private var lastObservedRequestKey: GlobalSearchRequestKey? = null
    private var pendingRestoreKey: GlobalSearchRequestKey? = null

    private val loadMoreScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dy <= 0 || !shouldLoadMore(recyclerView)) return
            viewModel.loadMore()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = StyleRegistry.getOrDefault(arguments?.getString(GlobalSearchActivity.STYLE_ID_KEY)) {
            GlobalSearchStyle.Builder(context).build()
        }
        session = GlobalSearchSessionResolver.require(arguments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = SceytFragmentGlobalSearchMediaBinding.bind(view)
        initViews()
        applyStyle()
        observeState()
    }

    override fun onPause() {
        saveScrollState()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.listRecyclerView.removeOnScrollListener(loadMoreScrollListener)
        binding.mediaRecyclerView.removeOnScrollListener(loadMoreScrollListener)
        _binding = null
        super.onDestroyView()
    }

    protected open fun createListAdapter(): GlobalSearchListAdapter {
        return GlobalSearchListAdapter(
            style = style,
            onChannelClick = hostActivity::onChannelClicked,
            onMessageClick = hostActivity::onMessageClicked,
            onAttachmentClick = { item -> hostActivity.onAttachmentClicked(item.result) }
        )
    }

    protected open fun createMediaGridAdapter(): GlobalSearchMediaGridAdapter {
        return GlobalSearchMediaGridAdapter { item ->
            hostActivity.onAttachmentClicked(item.result)
        }
    }

    protected open fun createListLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireContext())
    }

    protected open fun createGridLayoutManager(): RecyclerView.LayoutManager {
        return GridLayoutManager(requireContext(), 3)
    }

    protected open fun createViewModelFactory(session: GlobalSearchSession): ViewModelProvider.Factory {
        return GlobalSearchTabViewModelFactory(
            tab = GlobalSearchTab.Media,
            session = session,
            interactor = GlobalSearchLocalInteractor()
        )
    }

    protected open fun initViews() {
        binding.listRecyclerView.layoutManager = createListLayoutManager()
        binding.listRecyclerView.adapter = listAdapter
        binding.listRecyclerView.addOnScrollListener(loadMoreScrollListener)

        binding.mediaRecyclerView.layoutManager = createGridLayoutManager()
        binding.mediaRecyclerView.adapter = mediaAdapter
        binding.mediaRecyclerView.addOnScrollListener(loadMoreScrollListener)
        binding.mediaRecyclerView.addItemDecoration(
            RecyclerItemOffsetDecoration(1.dpToPx(), 1.dpToPx(), 1.dpToPx(), 1.dpToPx())
        )
    }

    protected open fun applyStyle() {
        style.emptyTitleTextStyle.apply(binding.tvEmptyTitle)
        style.emptySubtitleTextStyle.apply(binding.tvEmptySubtitle)
    }

    protected open fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    protected open fun render(state: GlobalSearchTabState) {
        if (state.requestKey != lastObservedRequestKey) {
            lastObservedRequestKey = state.requestKey
            pendingRestoreKey = state.requestKey
        }

        listAdapter.submit(state.listItems, state.query, state.showMessageChannel)
        mediaAdapter.submit(state.mediaGridItems)

        binding.progressBar.isVisible = state.isLoading
        binding.emptyStateGroup.isVisible = state.showEmptyState
        binding.listRecyclerView.isVisible = state.listItems.isNotEmpty() && !state.showMediaGrid
        binding.mediaRecyclerView.isVisible = state.showMediaGrid

        state.emptyState?.let { emptyState ->
            binding.ivEmpty.setImageResource(emptyState.iconRes)
            binding.tvEmptyTitle.setText(emptyState.titleRes)
            binding.tvEmptySubtitle.setText(emptyState.subtitleRes)
        }

        restoreScrollIfNeeded(state)
        requestMoreIfViewportNotFilled(state)
    }

    protected open fun restoreScrollIfNeeded(state: GlobalSearchTabState) {
        if (pendingRestoreKey != state.requestKey || state.isLoading) return

        currentRecyclerView(state).post {
            if (_binding == null || pendingRestoreKey != state.requestKey) return@post
            val recyclerView = currentRecyclerView(state)
            val layoutManager = recyclerView.layoutManager ?: return@post
            if (state.hasResults) {
                state.scrollState?.let(layoutManager::onRestoreInstanceState)
                    ?: recyclerView.scrollToPosition(0)
            }
            pendingRestoreKey = null
        }
    }

    protected open fun requestMoreIfViewportNotFilled(state: GlobalSearchTabState) {
        if (state.requestKey?.query?.isNotBlank() == true || state.isLoading || state.isLoadingMore || !state.hasMore) return
        val recyclerView = currentRecyclerView(state)
        recyclerView.post {
            if (_binding == null || !recyclerView.isVisible || recyclerView.canScrollVertically(1)) return@post
            viewModel.loadMore()
        }
    }

    protected open fun currentRecyclerView(state: GlobalSearchTabState): RecyclerView {
        return if (state.showMediaGrid) binding.mediaRecyclerView else binding.listRecyclerView
    }

    protected open fun shouldLoadMore(recyclerView: RecyclerView): Boolean {
        val layoutManager = recyclerView.layoutManager ?: return !recyclerView.canScrollVertically(1)
        val totalCount = layoutManager.itemCount
        if (totalCount == 0) return false

        val lastVisible = when (layoutManager) {
            is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
            is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition()
            else -> return !recyclerView.canScrollVertically(1)
        }
        return lastVisible >= totalCount - LOAD_MORE_THRESHOLD
    }

    protected open fun saveScrollState() {
        val currentState = viewModel.state.value
        val recyclerView = currentRecyclerView(currentState)
        viewModel.saveScrollState(recyclerView.layoutManager?.onSaveInstanceState())
    }
}
