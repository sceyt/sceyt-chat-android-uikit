package com.sceyt.chatuikit.presentation.components.create_poll

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.databinding.SceytFragmentCreatePollBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.extensions.setOnlyClickable
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.logicimpl.message.ChannelId
import com.sceyt.chatuikit.presentation.components.create_poll.adapters.CreatePollOptionsAdapter
import com.sceyt.chatuikit.presentation.components.create_poll.adapters.PollOptionItemAnimator
import com.sceyt.chatuikit.shared.helpers.ReorderSwipeController
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.create_poll.CreatePollStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel

open class CreatePollFragment : Fragment(), SceytKoinComponent {
    protected lateinit var binding: SceytFragmentCreatePollBinding
    protected lateinit var style: CreatePollStyle
    protected val viewModel: CreatePollViewModel by viewModel()
    protected var pollOptionsAdapter: CreatePollOptionsAdapter? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = StyleRegistry.getOrDefault(arguments?.getString(STYLE_ID_KEY)) {
            CreatePollStyle.Builder(context, null).build()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = SceytFragmentCreatePollBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyStyle()
        initViewModel()
        initViews()
        initPollOptionsAdapter()
    }

    protected open fun initViewModel() {
        viewModel.uiState
            .flowWithLifecycle(lifecycle = lifecycle)
            .onEach(::onUiStateChange)
            .launchIn(lifecycleScope)
    }

    protected fun initViews() = with(binding) {
        toolbar.setNavigationClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Setup question input
        etQuestion.doAfterTextChanged {
            viewModel.updateQuestion(it?.toString().orEmpty())
        }

        // Setup switches
        switchAnonymous.setOnlyClickable()
        switchMultipleVotes.setOnlyClickable()

        switchAnonymous.setOnClickListener {
            viewModel.toggleAnonymous()
        }

        switchMultipleVotes.setOnClickListener {
            viewModel.toggleMultipleVotes()
        }

        tvAddOption.setOnClickListener {
            viewModel.addOption(true)
        }

        btnSend.setOnClickListener {
            val channelId = arguments?.getLong(CHANNEL_ID_KEY) ?: 0L
            val success = viewModel.createPoll(channelId)
            if (success) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    protected open fun initPollOptionsAdapter() {
        pollOptionsAdapter = CreatePollOptionsAdapter(
            style = style,
            onTextChanged = { option, text ->
                viewModel.updateOption(option.id, text)
            },
            onRemoveClick = { option ->
                viewModel.removeOption(option.id)
            },
            onOptionMoved = { fromPosition, toPosition ->
                viewModel.moveOption(fromPosition, toPosition)
            },
            onNextClick = {
                viewModel.onNextOptionClick(it)
            },
            onOptionClick = { view, option ->
                viewModel.onOptionClick(view, option)
            }
        )

        with(binding.rvOptions) {
            adapter = pollOptionsAdapter
            itemAnimator = PollOptionItemAnimator()

            // Setup ItemTouchHelper for drag and drop
            ReorderSwipeController(onMoveItem = { fromPosition, toPosition ->
                pollOptionsAdapter?.moveItem(fromPosition, toPosition)
            }).attachToRecyclerView(this)
        }
    }

    protected open fun onUiStateChange(state: CreatePollUIState) = with(binding) {
        switchAnonymous.isChecked = state.isAnonymous
        switchMultipleVotes.isChecked = state.allowMultipleVotes

        tvAddOption.isVisible = !state.reachedMaxPollCount
        updateSendButtonState(state.isValid)

        if (pollOptionsAdapter == null) {
            initPollOptionsAdapter()
        }
        pollOptionsAdapter?.submitList(state.options)

        state.error?.let {
            customToastSnackBar(root, it)
        }
    }

    protected open fun updateSendButtonState(isEnabled: Boolean) {
        with(binding.btnSend) {
            this.isEnabled = isEnabled
            alpha = if (isEnabled) 1.0f else 0.5f
        }
    }

    protected fun applyStyle() = with(binding) {
        root.setBackgroundColor(style.backgroundColor)

        // Set texts
        toolbar.setTitle(style.toolbarTitle)
        tvQuestionTitle.text = style.questionTitle
        etQuestion.hint = style.questionHint
        tvOptionsTitle.text = style.optionsTitle
        tvAddOption.text = style.addOptionTitle
        tvParametersTitle.text = style.parametersTitle
        switchAnonymous.text = style.anonymousPollTitle
        switchMultipleVotes.text = style.multipleVotesTitle

        // Apply text styles
        style.questionTitleTextStyle.apply(tvQuestionTitle)
        style.optionsTitleTextStyle.apply(tvOptionsTitle)
        style.addOptionTextStyle.apply(tvAddOption)
        style.parametersTitleTextStyle.apply(tvParametersTitle)
        style.questionInputTextStyle.apply(etQuestion, layoutQuestion)

        // Apply switch styles
        with(style.switchStyle) {
            apply(switchAnonymous)
            apply(switchMultipleVotes)
        }

        // Apply icons
        tvAddOption.setDrawableStart(style.addOptionIcon)
    }

    companion object {
        private const val STYLE_ID_KEY = "STYLE_ID_KEY"
        private const val CHANNEL_ID_KEY = "CHANNEL_ID_KEY"

        fun newInstance(
            channelId: ChannelId,
            styleId: String? = null,
        ) = CreatePollFragment().apply {
            arguments = Bundle().apply {
                putString(STYLE_ID_KEY, styleId)
                putLong(CHANNEL_ID_KEY, channelId)
            }
        }
    }
}

