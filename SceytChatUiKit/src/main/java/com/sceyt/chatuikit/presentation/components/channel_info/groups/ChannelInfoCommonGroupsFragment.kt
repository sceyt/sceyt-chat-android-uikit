package com.sceyt.chatuikit.presentation.components.channel_info.groups

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInfoCommonGroupsBinding
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.screenHeightPx
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.CommonGroupListItem
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.CommonGroupViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.CommonGroupsAdapter
import com.sceyt.chatuikit.presentation.components.channel_info.groups.viewmodel.ChannelInfoCommonGroupsViewModel
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.channel_info.common_groups.ChannelInfoCommonGroupsStyle
import com.sceyt.chatuikit.styles.extensions.channel_info.common_groups.setPageStatesView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

open class ChannelInfoCommonGroupsFragment : Fragment(), SceytKoinComponent {
    protected var commonGroupsAdapter: CommonGroupsAdapter? = null
    protected var binding: SceytFragmentChannelInfoCommonGroupsBinding? = null
    protected lateinit var channel: SceytChannel
        private set
    private lateinit var peerUserId: String
    protected lateinit var style: ChannelInfoCommonGroupsStyle
        private set
    protected open var pageStateView: PageStateView? = null

    protected val viewModel by viewModel<ChannelInfoCommonGroupsViewModel> {
        parametersOf(peerUserId)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getBundleArguments()
        initStyle(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return SceytFragmentChannelInfoCommonGroupsBinding.inflate(
            inflater,
            container,
            false
        ).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.applyStyle()
        addPageStateView()
        initViewModel()
    }

    protected open fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(CHANNEL_KEY))
        peerUserId = getPeerUserId()
    }

    protected open fun initStyle(context: Context) {
        val styleId = arguments?.getString(STYLE_ID_KEY)
        style = StyleRegistry.getOrDefault(styleId) {
            ChannelInfoCommonGroupsStyle.Builder(context, null).build()
        }
    }

    private fun addPageStateView() {
        binding?.root?.addView(PageStateView(requireContext()).apply {
            setPageStatesView(style)
            pageStateView = this

            post {
                (activity as? ChannelInfoActivity)?.getViewPagerY()?.let {
                    if (it > 0)
                        layoutParams.height = screenHeightPx() - it
                }
            }
        })
    }

    protected open fun initViewModel() {
        lifecycleScope.launch {
            viewModel.groupsFlow.collect(::onInitialGroupsList)
        }

        lifecycleScope.launch {
            viewModel.loadMoreFlow.collect(::onMoreGroupsList)
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner, ::onPageStateChange)
    }

    protected open fun onInitialGroupsList(list: List<CommonGroupListItem>) {
        if (commonGroupsAdapter == null) {
            val adapter = CommonGroupsAdapter(
                viewHolderFactory = CommonGroupViewHolderFactory(requireContext(), style).also {
                    it.setOnClickListener { _, channel ->
                        onChannelClick(channel)
                    }
                }
            ).also { commonGroupsAdapter = it }

            (binding ?: return).rvCommonGroups.apply {
                this.adapter = adapter

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (isLastItemDisplaying() && viewModel.canLoadMore()) {
                            viewModel.loadMore()
                        }
                    }
                })
            }

            adapter.submitList(list)
        } else {
            commonGroupsAdapter?.submitList(list)
        }
    }

    protected open fun onMoreGroupsList(list: List<CommonGroupListItem>) {
        commonGroupsAdapter?.addItems(list)
    }

    protected open fun onPageStateChange(pageState: PageState) {
        pageStateView?.updateState(
            state = pageState,
            showLoadingIfNeed = (commonGroupsAdapter?.itemCount ?: 0) == 0,
            enableErrorSnackBar = false
        )
    }

    protected open fun onChannelClick(channel: SceytChannel) {
        ChannelActivity.launch(requireContext(), channel)
    }

    private fun SceytFragmentChannelInfoCommonGroupsBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
    }

    protected open fun getPeerUserId(): String {
        return if (channel.isGroup.not()) {
            channel.members?.firstOrNull { it.id != SceytChatUIKit.chatUIFacade.myId }?.id ?: ""
        } else {
            ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commonGroupsAdapter = null
    }

    companion object {
        private const val CHANNEL_KEY = "CHANNEL"
        private const val STYLE_ID_KEY = "STYLE_ID_KEY"

        fun newInstance(
            channel: SceytChannel,
            styleId: String,
        ): ChannelInfoCommonGroupsFragment {
            return ChannelInfoCommonGroupsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(CHANNEL_KEY, channel)
                    putString(STYLE_ID_KEY, styleId)
                }
            }
        }
    }
}