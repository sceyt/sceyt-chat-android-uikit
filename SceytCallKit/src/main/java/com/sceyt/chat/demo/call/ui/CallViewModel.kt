package com.sceyt.chat.demo.call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callclient.call.data.CallPermissions
import com.callclient.call.data.SceytCallResult
import com.callclient.call.data.onFailure
import com.sceyt.audiorouting.AudioDevice
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.manager.CallUiState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the call UI screens.
 * Exposes CallManager state as StateFlows and provides action methods for UI interactions.
 */
class CallViewModel(
    private val callManager: CallManager
) : ViewModel() {

    val callUiState: StateFlow<CallUiState> = callManager.callUiState

    private val _errors = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val errors = _errors.asSharedFlow()

    val formattedDuration: StateFlow<String> = callManager.callDuration
        .map { formatDuration(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "00:00")

    val callDuration: StateFlow<Long> = callManager.callDuration

    val availableAudioDevices: StateFlow<List<AudioDevice>> = callManager.availableAudioDevices

    val selectedAudioDevice: StateFlow<AudioDevice?> = callManager.selectedAudioDevice

    // ========== UI Actions ==========

    fun onAnswerClick() {
        viewModelScope.launch { callManager.answerIncomingCall() }
    }

    fun onDeclineClick() {
        callManager.declineIncomingCall()
    }

    fun onEndCallClick() {
        callManager.endCall()
    }

    fun onCallMemberClick(id: String) {
        callManager.reinvite(id)
    }

    fun onToggleMute(): Boolean = callManager.toggleMute()

    fun onToggleCamera(): Boolean = callManager.toggleCamera()

    private var cameraDisabledForBackground = false

    fun onAppBackground() {
        if (callUiState.value.localParticipant?.isVideoEnabled == true) {
            cameraDisabledForBackground = true
            callManager.setCameraEnabled(false)
        }
    }

    fun onAppForeground() {
        if (cameraDisabledForBackground) {
            cameraDisabledForBackground = false
            callManager.setCameraEnabled(true)
        }
    }

    fun onSwitchCamera() {
        callManager.switchCamera()
    }

    fun onToggleSpeaker(): Boolean = callManager.toggleSpeaker()

    fun onSelectAudioDevice(device: AudioDevice) {
        callManager.selectAudioDevice(device)
    }

    fun onCallAgain() {
        viewModelScope.launch { callManager.callAgain() }
    }

    fun sendRinging() {
        callManager.sendRinging()
    }

    fun refreshAudioDevices() {
        callManager.refreshAudioDevices()
    }

    fun onUpdateCallPermissions(permissions: CallPermissions) {
        viewModelScope.launch {
            callManager.updateCallPermissions(permissions)
                .emitFailure("Failed to update permissions")
        }
    }

    fun onMuteAllParticipants() {
        viewModelScope.launch {
            callManager.muteAllRemoteParticipants()
                .emitFailure("Failed to mute participants")
        }
    }

    fun onDisableAllVideo() {
        viewModelScope.launch {
            callManager.disableAllRemoteParticipantsVideo()
                .emitFailure("Failed to disable video")
        }
    }

    // ========== Helpers ==========

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    private suspend fun <T> SceytCallResult<T>.emitFailure(
        fallbackMessage: String
    ) {
        onFailure { error ->
            val details = error.message?.takeIf { it.isNotBlank() }
            _errors.emit(details?.let { "$fallbackMessage: $it" } ?: fallbackMessage)
        }
    }
}
