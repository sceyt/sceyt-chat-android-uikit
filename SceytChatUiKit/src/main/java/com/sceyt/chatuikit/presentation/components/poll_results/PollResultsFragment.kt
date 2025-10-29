package com.sceyt.chatuikit.presentation.components.poll_results

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytFragmentPollResultsBinding
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultsAdapter
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders.PollResultsViewHolderFactory
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.PollResultClickListeners
import com.sceyt.chatuikit.presentation.components.poll_results.viewmodel.PollResultsUIState
import com.sceyt.chatuikit.presentation.components.poll_results.viewmodel.PollResultsViewModel
import com.sceyt.chatuikit.styles.StyleRegistry
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

open class PollResultsFragment : Fragment(), SceytKoinComponent {
    protected lateinit var binding: SceytFragmentPollResultsBinding
    protected lateinit var message: SceytMessage
    protected lateinit var style: PollResultsStyle
    protected val viewModel: PollResultsViewModel by viewModel {
        parametersOf(message)
    }

    private var pollResultsAdapter: PollResultsAdapter? = null
    private val myId: String? get() = SceytChatUIKit.chatUIFacade.myId

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initStyle(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SceytFragmentPollResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initToolbar()
        setupRecyclerView()
        observeUIState()
    }

    protected open fun getBundleArguments() {
        message = requireNotNull(arguments?.parcelable(MESSAGE))
    }

    protected open fun initStyle(context: Context) {
        style = StyleRegistry.getOrDefault(arguments?.getString(STYLE_ID_KEY)) {
            PollResultsStyle.Builder(context, null).build()
        }

    }

    protected open fun initToolbar() {
        style.toolbarStyle.apply(binding.toolbar)
        binding.toolbar.setTitle(style.toolbarTitle)
        binding.toolbar.setNavigationClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    protected open fun setupRecyclerView() {
        val factory = PollResultsViewHolderFactory(
            context = requireContext(),
            style = style,
        ).also {
            it.setOnClickListener(object : PollResultClickListeners.ClickListeners {
                override fun onShowAllClick(view: View, item: PollResultItem.PollOptionItem) {
                    onShowAllClick(item)
                }

                override fun onVoterClick(view: View, item: VoterItem.Voter) {
                    onVoterClick(item)
                }
            })
        }

        pollResultsAdapter = PollResultsAdapter(viewHolderFactory = factory)
        binding.rvPollOptions.adapter = pollResultsAdapter
    }

    protected open fun observeUIState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is PollResultsUIState.Success -> {
                                onStateSuccess(state.items)
                            }

                            is PollResultsUIState.Loading -> {
                                onStateLoading()
                            }

                            is PollResultsUIState.Empty -> {
                                onStateEmpty()
                            }
                        }
                    }
                }

                launch {
                    viewModel.findOrCreateChatFlow.collect { channel ->
                        onFindOrCreateChat(channel)
                    }
                }
            }
        }
    }

    protected open fun onStateSuccess(items: List<PollResultItem>) {
        pollResultsAdapter?.submitList(items)
    }

    protected open fun onStateLoading() {
    }

    protected open fun onStateEmpty() {
    }

    protected open fun onShowAllClick(item: PollResultItem.PollOptionItem) {
        val pollId = message.poll?.id ?: return
        val pollOptionId = item.pollOption.id
        val fragment = PollOptionVotersFragment.newInstance(
            pollId = pollId,
            pollOptionId = pollOptionId,
            pollOptionName = item.pollOption.name,
            pollOptionVotersCount = item.voteCount,
            styleId = style.styleId
        )

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right,
                0,
                0,
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_out_right
            )
            .replace(com.sceyt.chatuikit.R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    protected open fun onVoterClick(item: VoterItem.Voter) {
        val user = item.vote.user ?: return
        if (user.id == myId) return
        viewModel.findOrCreatePendingDirectChat(user)
    }

    protected open fun onFindOrCreateChat(sceytChannel: SceytChannel) {
        ChannelInfoActivity.launch(requireContext(), sceytChannel)
    }

    companion object {
        private const val MESSAGE = "MESSAGE"
        private const val STYLE_ID_KEY = "STYLE_ID_KEY"

        fun newInstance(
                message: SceytMessage,
                styleId: String?
        ): PollResultsFragment {
            val fragment = PollResultsFragment()
            fragment.setBundleArguments {
                putParcelable(MESSAGE, message)
                putString(STYLE_ID_KEY, styleId)
            }
            return fragment
        }
    }
}