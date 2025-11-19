package com.sceyt.chatuikit.presentation.components.poll_results.option_voters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytFragmentPollOptionVotersBinding
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VotersAdapter
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders.VotersViewHolderFactory
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.VoterClickListeners
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.poll_results.PollOptionVotersStyle
import com.sceyt.chatuikit.styles.poll_results.PollResultsStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

open class PollOptionVotersFragment : Fragment(), SceytKoinComponent {
    protected var votersAdapter: VotersAdapter? = null
    protected lateinit var binding: SceytFragmentPollOptionVotersBinding
    protected lateinit var message: SceytMessage
        private set
    protected lateinit var pollOptionId: String
        private set
    protected val pollOptionName: String
        get() = message.poll?.options?.firstOrNull { it.id == pollOptionId }?.name.orEmpty()
    protected lateinit var style: PollOptionVotersStyle
        private set
    protected val viewModel: PollOptionVotersViewModel by viewModel {
        parametersOf(
            message,
            pollOptionId
        )
    }
    private var styleId: String = ""
    private val myId: String? get() = SceytChatUIKit.chatUIFacade.myId

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
        binding = SceytFragmentPollOptionVotersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyStyle()
        initViews()
        setupVotersAdapter()
        initViewModel()
    }

    protected open fun getBundleArguments() {
        message = requireNotNull(arguments?.parcelable(MESSAGE))
        pollOptionId = requireNotNull(arguments?.getString(POLL_OPTION_ID))
        styleId = requireNotNull(arguments?.getString(STYLE_ID))
    }

    protected open fun initStyle(context: Context) {
        val pollStyle = StyleRegistry.getOrDefault(STYLE_ID) {
            PollResultsStyle.Builder(context, null).build()
        }
        style = pollStyle.pollOptionVotersStyle
    }

    protected open fun initViews() {
        binding.toolbar.setNavigationClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    protected open fun setupVotersAdapter() {
        votersAdapter = VotersAdapter(
            viewHolderFactory = VotersViewHolderFactory(requireContext(), style).also {
                it.setOnClickListener(VoterClickListeners.VoterClickListener { _, item ->
                    onVoterClick(item)
                })
            }
        )

        binding.rvVoters.apply {
            adapter = votersAdapter
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 100
                removeDuration = 100
                moveDuration = 100
            }

            addRVScrollListener(onScrolled = { _: RecyclerView, _: Int, _: Int ->
                if (isLastItemDisplaying()) {
                    if (viewModel.canLoadMore()) {
                        viewModel.loadMore()
                    }
                }
            })
        }
    }

    protected open fun initViewModel() {
        viewModel.uiState
            .flowWithLifecycle(lifecycle = viewLifecycleOwner.lifecycle)
            .onEach(::onUiStateChange)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.findOrCreateChatFlow
            .flowWithLifecycle(lifecycle = viewLifecycleOwner.lifecycle)
            .onEach(::onFindOrCreateChat)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    protected open fun onUiStateChange(state: PollOptionVotersUIState) {
        setOrUpdateVotersAdapter(state.voters)

        // Show loading only when there are no voters yet
        if (state.isLoading && state.voters.isEmpty()) {
            showLoading()
        } else {
            hideLoading()
        }

        // Handle errors if needed
        state.error?.let {
            // Could show error message here if needed
        }
    }

    protected open fun setOrUpdateVotersAdapter(voters: List<VoterItem>) {
        votersAdapter?.submitList(voters)
    }

    protected open fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvVoters.visibility = View.GONE
    }

    protected open fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.rvVoters.visibility = View.VISIBLE
    }

    protected open fun onVoterClick(item: VoterItem.Voter) {
        val user = item.vote.user ?: return
        if (user.id == myId) return
        viewModel.findOrCreatePendingDirectChat(user)
    }

    protected open fun onFindOrCreateChat(sceytChannel: SceytChannel) {
        ChannelInfoActivity.Companion.launch(requireContext(), sceytChannel)
    }

    protected open fun applyStyle() = with(binding) {
        root.setBackgroundColor(style.backgroundColor)

        // Apply toolbar style
        style.toolbarStyle.apply(toolbar)
        toolbar.setTitle(pollOptionName)

        // Apply loader color
        progressBar.setProgressColor(style.initialLoaderColor)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        votersAdapter = null
    }

    companion object {
        private const val MESSAGE = "MESSAGE"
        private const val POLL_OPTION_ID = "POLL_OPTION_ID"
        private const val STYLE_ID = "STYLE_ID"

        fun newInstance(
            message: SceytMessage,
            pollOptionId: String,
            styleId: String
        ): PollOptionVotersFragment {
            val fragment = PollOptionVotersFragment()
            fragment.setBundleArguments {
                putParcelable(MESSAGE, message)
                putString(POLL_OPTION_ID, pollOptionId)
                putString(STYLE_ID, styleId)
            }
            return fragment
        }
    }
}