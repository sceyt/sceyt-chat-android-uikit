package com.sceyt.chatuikit.presentation.components.create_poll

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PollOptionItem(
        val id: String = java.util.UUID.randomUUID().toString(),
        val text: String = "",
        val isCurrent: Boolean = false,
)

data class CreatePollUIState(
        val question: String = "",
        val options: List<PollOptionItem> = listOf(PollOptionItem("1"), PollOptionItem("2")),
        val isAnonymous: Boolean = false,
        val allowMultipleVotes: Boolean = false,
        val canRetractVotes: Boolean = true,
        val reachedMaxPollCount: Boolean = false,
        val isValid: Boolean = false,
        val error: String? = null,
)

class CreatePollViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePollUIState())
    val uiState = _uiState.asStateFlow()

    private val debounceHelper by lazy { DebounceHelper(200L, viewModelScope) }
    private val maxPollCount = 12

    fun updateQuestion(question: String) {
        _uiState.update {
            it.copy(question = question, error = null)
        }
        validatePoll()
    }

    fun updateOption(optionId: String, text: String) {
        debounceHelper.submit {
            Log.i("sdfsdf", "updateOption: $optionId : $text")
            _uiState.update { state ->
                val updatedOptions = state.options.map { option ->
                    if (option.id == optionId)
                        option.copy(text = text)
                    else option
                }
                state.copy(options = updatedOptions, error = null)
            }
            validatePoll()
        }
    }

    fun addOption(makeCurrent: Boolean) {
        _uiState.update { state ->
            if (state.reachedMaxPollCount) return

            val newOption = PollOptionItem(isCurrent = makeCurrent)
            val updatedOptions = if (makeCurrent) {
                state.options.map { it.copy(isCurrent = false) } + newOption
            } else {
                state.options + newOption
            }
            val reachedMax = updatedOptions.size >= maxPollCount
            state.copy(options = updatedOptions, reachedMaxPollCount = reachedMax, error = null)
        }
    }

    fun removeOption(optionId: String) {
        _uiState.update { state ->
            val options = state.options.toArrayList()
            val index = state.options.indexOfFirst { it.id == optionId }
            if (index == -1) return@update state

            // If the removed option is the current one, set the previous option as current
            if (options[index].isCurrent && options.size > 1) {
                val newCurrentIndex = if (index - 1 >= 0)
                    index - 1 else 0

                options[newCurrentIndex] = options[newCurrentIndex].copy(isCurrent = true)
            }
            // Remove the option
            options.removeAt(index)
            val reachedMax = options.size >= maxPollCount
            state.copy(options = options.toList(), reachedMaxPollCount = reachedMax, error = null)
        }
        validatePoll()
    }

    fun moveOption(fromPosition: Int, toPosition: Int) {
        _uiState.update { state ->
            val mutableOptions = state.options.toMutableList()
            if (fromPosition in mutableOptions.indices && toPosition in mutableOptions.indices) {
                val item = mutableOptions.removeAt(fromPosition)
                mutableOptions.add(toPosition, item)
            }
            state.copy(options = mutableOptions)
        }
    }

    fun onNextOptionClick(it: PollOptionItem) {
        val currentIndex = _uiState.value.options.indexOfFirst { option -> option.id == it.id }
        if (currentIndex != -1) {
            val nextIndex = currentIndex + 1
            if (nextIndex < _uiState.value.options.size) {
                //move focus to next option
                _uiState.update { state ->
                    val updatedOptions = state.options.map { option ->
                        if (option.id == state.options[nextIndex].id) {
                            option.copy(isCurrent = true)
                        } else option.copy(isCurrent = false)
                    }
                    state.copy(options = updatedOptions)
                }
            } else {
                addOption(true)
            }
        }
    }

    fun onOptionClick(view: android.widget.EditText, option: PollOptionItem) {
        view.requestFocus()
        view.setSelection(view.text.length)
        _uiState.update { state ->
            //mark option as current
            val updatedOptions = state.options.map { opt ->
                if (opt.id == option.id) {
                    if (opt.isCurrent)
                        return@update state
                    opt.copy(isCurrent = true)
                } else opt.copy(isCurrent = false)
            }
            state.copy(options = updatedOptions)
        }
    }

    fun toggleAnonymous() {
        _uiState.update {
            it.copy(isAnonymous = !it.isAnonymous)
        }
    }

    fun toggleMultipleVotes() {
        _uiState.update {
            it.copy(allowMultipleVotes = !it.allowMultipleVotes)
        }
    }

    fun toggleCanRetractVotes() {
        _uiState.update {
            it.copy(canRetractVotes = !it.canRetractVotes)
        }
    }

    fun createPoll(): Boolean {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Please fill in all required fields") }
            return false
        }
        // TODO: Implement actual poll creation logic
        return true
    }

    private fun validatePoll() {
        _uiState.update { state ->
            val isQuestionValid = state.question.isNotBlank()
            val validOptions = state.options.filter { it.text.isNotBlank() }
            val areOptionsValid = validOptions.size >= 2

            state.copy(isValid = isQuestionValid && areOptionsValid)
        }
    }
}

