package com.sceyt.chatuikit.presentation.components.invite_link.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.persistence.repositories.ChannelInviteKeyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    data class Success(val channel: SceytChannel) : UiState()
    data class Error(val error: Throwable?) : UiState()
}

class JoinWithInviteLinkViewModel(
        private val inviteLink: String,
        private val channelInviteKeyRepository: ChannelInviteKeyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        getChannelByInviteLink()
    }

    private fun getChannelByInviteLink() = viewModelScope.launch {
        val inviteKey = inviteLink.substringAfterLast("/")
        channelInviteKeyRepository.getChannelByInviteKey(inviteKey).fold(
            onSuccess = { channel ->
                channel?.let {
                    _uiState.value = UiState.Success(it)
                } ?: run {
                    _uiState.value = UiState.Error(Exception("Channel not found"))
                }
            },
            onError = {
                _uiState.value = UiState.Error(it)
            }
        )
    }
}