package com.sceyt.chatuikit.presentation.components.invite_link.join

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.persistence.interactor.ChannelInviteKeyInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

sealed class UiState {
    object Loading : UiState()
    data class Success(
            val inviteKey: String,
            val channel: SceytChannel,
    ) : UiState()

    data class Error(val error: Throwable?) : UiState()
}

sealed class JoinActionState {
    data object Idle : JoinActionState()
    data object Joining : JoinActionState()
    data class Joined(val channel: SceytChannel) : JoinActionState()
    data class JoinError(val error: Throwable?) : JoinActionState()
}

class JoinByInviteLinkViewModel(
        private val inviteLink: Uri,
        private val channelInviteKeyInteractor: ChannelInviteKeyInteractor,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _joinActionState = MutableStateFlow<JoinActionState>(JoinActionState.Idle)
    val joinActionState = _joinActionState.asStateFlow()

    init {
        getChannelByInviteLink()
    }

    private fun getChannelByInviteLink() = viewModelScope.launch {
        val inviteKey = inviteLink.lastPathSegment ?: run {
            _uiState.value = UiState.Error(Exception("Invalid invite link"))
            return@launch
        }

        ConnectionEventManager.awaitToConnectSceytWithTimeout(1.minutes.inWholeMilliseconds)

        channelInviteKeyInteractor.getChannelByInviteKey(inviteKey).fold(
            onSuccess = { channel ->
                if (channel == null) {
                    _uiState.value = UiState.Error(Exception("Channel not found"))
                    return@fold
                }
                _uiState.value = UiState.Success(inviteKey, channel)
            },
            onError = {
                _uiState.value = UiState.Error(it)
            }
        )
    }

    fun joinToChannel() {
        val state = _uiState.value
        if (state is UiState.Success) {
            viewModelScope.launch {
                val inviteKey = inviteLink.lastPathSegment ?: run {
                    _joinActionState.value = JoinActionState.JoinError(
                        error = Exception("Invalid invite link")
                    )
                    return@launch
                }
                _joinActionState.value = JoinActionState.Joining

                channelInviteKeyInteractor.joinWithInviteKey(inviteKey).fold(
                    onSuccess = { channel ->
                        if (channel == null) {
                            _joinActionState.value = JoinActionState.JoinError(
                                error = Exception("Failed to join channel")
                            )
                            return@fold
                        }
                        _joinActionState.value = JoinActionState.Joined(channel)
                    },
                    onError = { error ->
                        _joinActionState.value = JoinActionState.JoinError(error)
                    }
                )
            }
        }
    }
}