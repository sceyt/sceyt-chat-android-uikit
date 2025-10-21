package com.sceyt.chatuikit.presentation.components.create_poll

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytFragmentCreatePollBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.extensions.setOnlyClickable
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.components.create_poll.adapters.PollOptionsAdapter
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.create_poll.CreatePollStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel

open class CreatePollFragment : Fragment(), SceytKoinComponent {
    protected lateinit var binding: SceytFragmentCreatePollBinding
    protected lateinit var style: CreatePollStyle
    protected val viewModel: CreatePollViewModel by viewModel()
    protected lateinit var pollOptionsAdapter: PollOptionsAdapter

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
    }

    protected open fun initViewModel() {
        viewModel.uiState.onEach(::onUiStateChange).launchIn(lifecycleScope)
    }

    protected fun initViews() = with(binding) {
        // Setup RecyclerView
        pollOptionsAdapter = PollOptionsAdapter(
            style = style,
            onTextChanged = { option, text ->
                viewModel.updateOption(option.id, text)
            },
            onRemoveClick = { option ->
                viewModel.removeOption(option.id)
            },
            onOptionMoved = { fromPosition, toPosition ->
                viewModel.moveOption(fromPosition, toPosition)
            }
        )
        rvOptions.adapter = pollOptionsAdapter

        // Setup ItemTouchHelper for drag and drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                pollOptionsAdapter.moveItem(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
        })
        itemTouchHelper.attachToRecyclerView(rvOptions)

        // Setup question input
        etQuestion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateQuestion(s?.toString() ?: "")
            }
        })

        // Setup switches
        switchAnonymous.setOnlyClickable()
        switchMultipleVotes.setOnlyClickable()
        switchRetractVotes.setOnlyClickable()

        switchAnonymous.setOnClickListener {
            viewModel.toggleAnonymous()
        }

        switchMultipleVotes.setOnClickListener {
            viewModel.toggleMultipleVotes()
        }

        switchRetractVotes.setOnClickListener {
            viewModel.toggleCanRetractVotes()
        }

        tvAddOption.setOnClickListener {
            viewModel.addOption()
        }
    }

    protected open fun onUiStateChange(state: CreatePollUIState) {
        binding.switchAnonymous.isChecked = state.isAnonymous
        binding.switchMultipleVotes.isChecked = state.allowMultipleVotes
        binding.switchRetractVotes.isChecked = state.canRetractVotes

        pollOptionsAdapter.submitList(state.options)

        state.error?.let {
            customToastSnackBar(binding.root, it)
        }
    }

    fun getPollData(): CreatePollUIState? {
        return if (viewModel.uiState.value.isValid) {
            viewModel.uiState.value
        } else {
            viewModel.createPoll() // This will show validation errors
            null
        }
    }

    protected fun applyStyle() = with(binding) {
        root.setBackgroundColor(style.backgroundColor)

        // Set texts
        tvQuestionTitle.text = style.questionTitle
        etQuestion.hint = style.questionHint
        tvOptionsTitle.text = style.optionsTitle
        tvAddOption.text = style.addOptionTitle
        tvParametersTitle.text = style.parametersTitle
        switchAnonymous.text = style.anonymousPollTitle
        switchMultipleVotes.text = style.multipleVotesTitle
        switchRetractVotes.text = style.retractVotesTitle

        // Apply text styles
        style.questionTitleTextStyle.apply(tvQuestionTitle)
        style.questionInputTextStyle.apply(etQuestion)
        style.optionsTitleTextStyle.apply(tvOptionsTitle)
        style.addOptionTextStyle.apply(tvAddOption)
        style.parametersTitleTextStyle.apply(tvParametersTitle)

        // Apply switch styles
        with(style.switchStyle) {
            apply(switchAnonymous)
            apply(switchMultipleVotes)
            apply(switchRetractVotes)
        }

        // Apply icons
        tvAddOption.setDrawableStart(style.addOptionIcon)

        // Apply backgrounds
        style.questionBackgroundStyle.apply(layoutQuestion)
    }

    companion object {
        private const val STYLE_ID_KEY = "STYLE_ID_KEY"

        fun newInstance(styleId: String?) = CreatePollFragment().apply {
            arguments = Bundle().apply {
                putString(STYLE_ID_KEY, styleId)
            }
        }
    }
}

