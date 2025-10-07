package com.sceyt.chatuikit.presentation.components.invite_link

import android.content.Context
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InviteLinkUIState(
        val inviteLink: String? = null,
        val showPreviousMessages: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
)

class ChannelInviteLinkViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(InviteLinkUIState())
    val uiState = _uiState.asStateFlow()

    init {
        getInviteLink()
    }

    fun toggleShowPreviousMessages() {
        val currentSetting = _uiState.value.showPreviousMessages
        updateInviteLinkSettings(!currentSetting)
    }

    fun resetInviteLink() {
        // Logic to reset the invite link
        _uiState.update {
            it.copy(isLoading = true)
        }

        // Simulate resetting the invite link
        viewModelScope.launch {
            delay(1000)
            val newLink = "https://link.sceyt.com/newlink1234567"
            _uiState.update {
                it.copy(inviteLink = newLink, isLoading = false)
            }
        }
    }

    fun getInviteLink() {
        _uiState.update {
            it.copy(isLoading = true)
        }

        // Simulate fetching the invite link
        viewModelScope.launch {
            delay(1000)
            val fetchedLink = "https://link.sceyt.com/abcdefg1234567"
            _uiState.update {
                it.copy(inviteLink = fetchedLink, isLoading = false)
            }
        }
    }

    fun shareInviteLink(launchingContext: Context) {
        val link = _uiState.value.inviteLink ?: return

        ShareCompat.IntentBuilder(launchingContext)
            .setText(link)
            .setType("text/plain")
            .startChooser()
    }


    private fun updateInviteLinkSettings(showPreviousMessages: Boolean) {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            // Simulate network or database operation
            delay(1000)
            // After operation, update the state
            _uiState.update {
                it.copy(showPreviousMessages = showPreviousMessages, isLoading = false)
            }
        }
    }
}