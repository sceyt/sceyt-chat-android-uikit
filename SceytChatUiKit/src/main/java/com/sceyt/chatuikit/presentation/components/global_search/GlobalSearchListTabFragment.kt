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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytFragmentGlobalSearchListBinding
import com.sceyt.chatuikit.presentation.components.global_search.adapters.GlobalSearchListAdapter
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle
import kotlinx.coroutines.launch

abstract class GlobalSearchListTabFragment :
    Fragment(R.layout.sceyt_fragment_global_search_list) {

    private companion object {
        private const val LOAD_MORE_THRESHOLD = 6
    }

    protected abstract val tab: GlobalSearchTab

    protected open val hostActivity: GlobalSearchActivity
        get() = requireActivity() as? GlobalSearchActivity
            ?: error("GlobalSearchListTabFragment must be hosted by GlobalSearchActivity.")

    protected lateinit var session: GlobalSearchSession
    protected open val viewModel: GlobalSearchTabViewModel by viewModels {
        createViewModelFactory(session)
    }

    protected lateinit var style: GlobalSearchStyle

    private var _binding: SceytFragmentGlobalSearchListBinding? = null
    protected val binding: SceytFragmentGlobalSearchListBinding
        get() = checkNotNull(_binding)

    private val listAdapter by lazy(LazyThreadSafetyMode.NONE) { createListAdapter() }

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
        _binding = SceytFragmentGlobalSearchListBinding.bind(view)
        initViews()
        applyStyle()
        observeState()
    }

    override fun onDestroyView() {
        binding.recyclerView.removeOnScrollListener(loadMoreScrollListener)
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

    protected open fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireContext())
    }

    protected open fun createViewModelFactory(session: GlobalSearchSession): ViewModelProvider.Factory {
        return GlobalSearchTabViewModelFactory(
            tab = tab,
            session = session,
            interactor = GlobalSearchLocalInteractor()
        )
    }

    protected open fun initViews() {
        binding.recyclerView.layoutManager = createLayoutManager()
        binding.recyclerView.adapter = listAdapter
        binding.recyclerView.addOnScrollListener(loadMoreScrollListener)
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

        binding.recyclerView.isVisible = state.listItems.isNotEmpty()
        binding.progressBar.isVisible = state.isLoading
        binding.emptyStateGroup.isVisible = state.showEmptyState
        state.emptyState?.let { emptyState ->
            binding.ivEmpty.setImageResource(emptyState.iconRes)
            binding.tvEmptyTitle.setText(emptyState.titleRes)
            binding.tvEmptySubtitle.setText(emptyState.subtitleRes)
        }

        requestMoreIfViewportNotFilled(state)
    }

    protected open fun requestMoreIfViewportNotFilled(state: GlobalSearchTabState) {
        if (state.requestKey?.query?.isNotBlank() == true || state.isLoading || state.isLoadingMore || !state.hasMore) return

        binding.recyclerView.post {
            if (_binding == null || !binding.recyclerView.isVisible || binding.recyclerView.canScrollVertically(1)) return@post
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
}
