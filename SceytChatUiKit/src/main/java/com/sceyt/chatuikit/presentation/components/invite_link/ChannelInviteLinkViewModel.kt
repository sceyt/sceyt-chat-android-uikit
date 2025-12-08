package com.sceyt.chatuikit.presentation.components.invite_link

import android.content.Context
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.data.models.onError
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.interactor.ChannelInviteKeyInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InviteLinkUIState(
    val inviteKey: String? = null,
    val showPreviousMessages: Boolean = true,
    val jumpDrawablesToCurrentState: Boolean = false,
    val allowResetLink: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val inviteLink: String?
        get() = inviteKey?.let {
            SceytChatUIKit.config.channelLinkDeepLinkConfig?.buildInviteUrl(it).toString()
        }
}

class ChannelInviteLinkViewModel(
    private var channel: SceytChannel,
    private val channelInviteKeyInteractor: ChannelInviteKeyInteractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteLinkUIState(allowResetLink = !channel.isPublic()))
    val uiState = _uiState.asStateFlow()

    init {
        getInviteLink(isInitial = true)
        observeChannelEvents()
    }

    private fun observeChannelEvents() {
        ChannelEventManager.onChannelEventFlow.onEach {
            if (it is ChannelActionEvent.Updated && channel.uri != it.channel.uri) {
                channel = it.channel
                getInviteLink(false)
            }
        }.launchIn(viewModelScope)
    }

    fun toggleShowPreviousMessages() {
        val currentSetting = _uiState.value.showPreviousMessages
        updateInviteLinkSettings(!currentSetting)
    }

    fun resetInviteLink() {
        val state = _uiState.value
        if (state.isLoading || state.inviteKey == null) return

        _uiState.update {
            it.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            val key = state.inviteKey
            channelInviteKeyInteractor.regenerateChannelInviteKey(
                channelId = channel.id,
                key = key,
                deletePermanently = true
            ).onSuccessNotNull { data ->
                _uiState.update {
                    it.copy(
                        inviteKey = data.key,
                        jumpDrawablesToCurrentState = false,
                        isLoading = false,
                        error = null
                    )
                }
            }.onError { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = error?.message)
                }
            }
        }
    }

    private fun getInviteLink(isInitial: Boolean) {
        val primaryKey = channel.uri
        if (primaryKey.isNullOrBlank()) {
            _uiState.update {
                it.copy(isLoading = false, error = "Channel has no primary key")
            }
            return
        }
        _uiState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            channelInviteKeyInteractor.getChannelInviteKey(
                channelId = channel.id,
                key = primaryKey
            ).onSuccessNotNull { data ->
                _uiState.update {
                    it.copy(
                        inviteKey = data.key,
                        showPreviousMessages = data.accessPriorHistory,
                        jumpDrawablesToCurrentState = isInitial,
                        isLoading = false,
                        error = null
                    )
                }
            }.onError { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = error?.message)
                }
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
        val state = _uiState.value
        if (state.isLoading || state.inviteKey == null) return

        _uiState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            channelInviteKeyInteractor.updateInviteKeySettings(
                channelId = channel.id,
                key = state.inviteKey,
                accessPriorHistory = showPreviousMessages,
                expireAt = 0,
                maxUses = 0
            ).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            showPreviousMessages = showPreviousMessages,
                            jumpDrawablesToCurrentState = false,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onError = { error ->
                    _uiState.update { state ->
                        state.copy(isLoading = false, error = error?.message)
                    }
                }
            )
        }
    }
}