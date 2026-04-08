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
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.presentation.components.global_search.adapters.GlobalSearchListAdapter
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle
import kotlinx.coroutines.launch

open class ChatsSearchFragment : Fragment(R.layout.sceyt_fragment_global_search_list) {
    protected open val viewModel: ChatsSearchViewModel by viewModels {
        createViewModelFactory(session)
    }
    protected lateinit var session: GlobalSearchSession
    protected lateinit var style: GlobalSearchStyle

    private var _binding: SceytFragmentGlobalSearchListBinding? = null
    protected val binding: SceytFragmentGlobalSearchListBinding
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
        _binding = SceytFragmentGlobalSearchListBinding.bind(view)
        initViews()
        applyStyle()
        observeState()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    protected open fun createViewModelFactory(session: GlobalSearchSession): ViewModelProvider.Factory {
        return ChatsSearchViewModelFactory(session)
    }

    protected open fun createListAdapter(): GlobalSearchListAdapter {
        return GlobalSearchListAdapter(
            style = style,
            onChannelClick = hostActivity::onChannelClicked,
            onMessageClick = hostActivity::onMessageClicked,
            onAttachmentClick = { item -> hostActivity.onAttachmentClicked(item.result) }
        )
    }

    protected open val hostActivity: GlobalSearchActivity
        get() = requireActivity() as? GlobalSearchActivity
            ?: error("ChatsSearchFragment must be hosted by GlobalSearchActivity.")

    protected open fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireContext())
    }

    protected open fun initViews() {
        binding.recyclerView.layoutManager = createLayoutManager()
        binding.recyclerView.adapter = listAdapter
        binding.recyclerView.addRVScrollListener(onScrolled = { recyclerView, _, dy ->
            if (dy <= 0 || !shouldLoadMore(recyclerView)) return@addRVScrollListener
            viewModel.loadMore()
        })
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
        listAdapter.submit(state.listItems, state.query, state.showMessageChannel)
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
            if (_binding == null || !binding.recyclerView.isVisible
                || binding.recyclerView.canScrollVertically(1)
            ) return@post
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
