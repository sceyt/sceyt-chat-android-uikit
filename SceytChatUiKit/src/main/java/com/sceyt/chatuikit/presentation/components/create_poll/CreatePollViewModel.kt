package com.sceyt.chatuikit.presentation.components.create_poll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PollOption(
        val id: String = java.util.UUID.randomUUID().toString(),
        val text: String = ""
)

data class CreatePollUIState(
        val question: String = "",
        val options: List<PollOption> = listOf(PollOption(), PollOption()),
        val isAnonymous: Boolean = false,
        val allowMultipleVotes: Boolean = false,
        val canRetractVotes: Boolean = true,
        val isValid: Boolean = false,
        val error: String? = null,
)

class CreatePollViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePollUIState())
    val uiState = _uiState.asStateFlow()

    fun updateQuestion(question: String) {
        _uiState.update {
            it.copy(question = question, error = null)
        }
        validatePoll()
    }

    fun updateOption(optionId: String, text: String) {
        _uiState.update { state ->
            val updatedOptions = state.options.map { option ->
                if (option.id == optionId) option.copy(text = text)
                else option
            }
            state.copy(options = updatedOptions, error = null)
        }
        validatePoll()
    }

    fun addOption() {
        _uiState.update { state ->
            state.copy(options = state.options + PollOption(), error = null)
        }
    }

    fun removeOption(optionId: String) {
        _uiState.update { state ->
            val updatedOptions = state.options.filter { it.id != optionId }
            state.copy(options = updatedOptions, error = null)
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

