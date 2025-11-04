package com.sceyt.chatuikit.presentation.components.poll_results.option_voters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.Vote
import com.sceyt.chatuikit.databinding.SceytFragmentPollOptionVotersBinding
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.styles.poll_results.PollResultsStyle
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VotersAdapter
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders.VotersViewHolderFactory
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.VoterClickListeners
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.poll_results.PollOptionVotersStyle
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

open class PollOptionVotersFragment : Fragment(), SceytKoinComponent {
    protected var votersAdapter: VotersAdapter? = null
    protected lateinit var binding: SceytFragmentPollOptionVotersBinding
    protected var messageId: Long = 0L
        private set
    protected lateinit var pollId: String
        private set
    protected lateinit var pollOptionId: String
        private set
    protected lateinit var pollOptionName: String
        private set
    protected var pollOptionVotersCount: Int = 0
        private set
    protected var ownVote: Vote? = null
        private set
    protected lateinit var style: PollOptionVotersStyle
        private set
    protected val viewModel: PollOptionVotersViewModel by viewModel {
        parametersOf(
            messageId,
            pollId,
            pollOptionId,
            pollOptionVotersCount,
            ownVote
        )
    }
    private var styleId: String = ""
    private val myId: String? get() = SceytChatUIKit.chatUIFacade.myId
    private val loadMoreDebounce by lazy { DebounceHelper(100, this) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getBundleArguments()
        initStyle(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SceytFragmentPollOptionVotersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        setupVotersAdapter()
        initViewModel()
        viewModel.loadVotes(0)
    }

    protected open fun getBundleArguments() {
        messageId = requireNotNull(arguments?.getLong(MESSAGE_ID))
        pollId = requireNotNull(arguments?.getString(POLL_ID))
        pollOptionId = requireNotNull(arguments?.getString(POLL_OPTION_ID))
        pollOptionName = requireNotNull(arguments?.getString(POLL_OPTION_NAME))
        pollOptionVotersCount = requireNotNull(arguments?.getInt(POLL_OPTION_VOTERS_COUNT))
        styleId = requireNotNull(arguments?.getString(STYLE_ID))
        ownVote = arguments?.parcelable(OWN_VOTE)
    }

    protected open fun initStyle(context: Context) {
        val pollStyle = StyleRegistry.getOrDefault(STYLE_ID) {
            PollResultsStyle.Builder(context, null).build()
        }
        style = pollStyle.pollOptionVotersStyle
    }

    protected open fun initViews() {
        binding.toolbar.setTitle(pollOptionName)
        binding.toolbar.setNavigationClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        style.let { votersStyle ->
            votersStyle.toolbarStyle.apply(binding.toolbar)
            binding.root.setBackgroundColor(votersStyle.backgroundColor)
            binding.progressBar.setProgressColor(votersStyle.initialLoaderColor)
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
                    loadMoreDebounce.submit {
                        if (!viewModel.canLoadNext()) return@submit

                        val offset = votersAdapter?.getSkip() ?: 0
                        viewModel.loadVotes(offset)
                    }
                }
            })
        }
    }

    protected open fun initViewModel() {
        viewModel.loadVotersLiveData.observe(viewLifecycleOwner) { voters ->
            setOrUpdateVotersAdapter(voters)

            if (voters.isNotEmpty()) {
                hideLoading()
            }
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PageState.StateLoading -> {
                    if (state.isLoading && votersAdapter?.itemCount == 0) {
                        showLoading()
                    } else {
                        hideLoading()
                    }
                }

                else -> {
                    hideLoading()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.findOrCreateChatFlow.collect { channel ->
                    onFindOrCreateChat(channel)
                }
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        votersAdapter = null
    }

    companion object {
        private const val MESSAGE_ID = "MESSAGE_ID"
        private const val POLL_ID = "POLL_ID"
        private const val POLL_OPTION_ID = "POLL_OPTION_ID"
        private const val POLL_OPTION_NAME = "POLL_OPTION_NAME"
        private const val POLL_OPTION_VOTERS_COUNT = "POLL_OPTION_VOTERS_COUNT"
        private const val STYLE_ID = "STYLE_ID"
        private const val OWN_VOTE = "OWN_VOTE"

        fun newInstance(
                messageId: Long,
                pollId: String,
                pollOptionId: String,
                pollOptionName: String,
                pollOptionVotersCount: Int,
                styleId: String,
                ownVote: Vote? = null
        ): PollOptionVotersFragment {
            val fragment = PollOptionVotersFragment()
            fragment.setBundleArguments {
                putLong(MESSAGE_ID, messageId)
                putString(POLL_ID, pollId)
                putString(POLL_OPTION_ID, pollOptionId)
                putString(POLL_OPTION_NAME, pollOptionName)
                putInt(POLL_OPTION_VOTERS_COUNT, pollOptionVotersCount)
                putString(STYLE_ID, styleId)
                putParcelable(OWN_VOTE, ownVote)
            }
            return fragment
        }
    }
}