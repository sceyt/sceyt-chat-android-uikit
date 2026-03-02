package com.sceyt.chat.demo.call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.audiorouting.AudioDevice
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.MediaState
import com.sceyt.chat.demo.call.manager.RemoteParticipantInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    /**
     * Current call UI state for screen routing.
     */
    val callUiState: StateFlow<CallUiState> = callManager.callUiState

    /**
     * Current media state (mute, camera, speaker, video tracks).
     */
    val mediaState: StateFlow<MediaState> = callManager.mediaState

    /**
     * Call duration formatted as "MM:SS" or "HH:MM:SS".
     */
    val formattedDuration: StateFlow<String> = callManager.callDuration
        .map { formatDuration(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "00:00")

    /**
     * Raw call duration in seconds.
     */
    val callDuration: StateFlow<Long> = callManager.callDuration

    /**
     * Available audio devices for selection.
     */
    val availableAudioDevices: StateFlow<List<AudioDevice>> = callManager.availableAudioDevices

    /**
     * Currently selected audio device.
     */
    val selectedAudioDevice: StateFlow<AudioDevice?> = callManager.selectedAudioDevice

    /**
     * Remote participant info (name, avatar).
     */
    val remoteParticipant: StateFlow<RemoteParticipantInfo?> = callManager.remoteParticipant

    // ========== UI Actions ==========

    /**
     * Answer the incoming call.
     */
    fun onAnswerClick() {
        viewModelScope.launch {
            callManager.answerIncomingCall(
                ((callManager.callUiState.value as? CallUiState.Incoming)?.call) ?: return@launch
            )
        }
    }

    /**
     * Decline the incoming call.
     */
    fun onDeclineClick() {
        callManager.declineIncomingCall()
    }

    /**
     * End the current call.
     */
    fun onEndCallClick() {
        callManager.endCall()
    }

    /**
     * Toggle microphone mute.
     * @return New mute state
     */
    fun onToggleMute(): Boolean {
        return callManager.toggleMute()
    }

    /**
     * Toggle camera enabled state.
     * @return New camera state
     */
    fun onToggleCamera(): Boolean {
        return callManager.toggleCamera()
    }

    /**
     * Switch between front and back camera.
     */
    fun onSwitchCamera() {
        callManager.switchCamera()
    }

    /**
     * Toggle between speaker and earpiece/headset.
     * @return New speaker state
     */
    fun onToggleSpeaker(): Boolean {
        return callManager.toggleSpeaker()
    }

    /**
     * Select a specific audio output device.
     */
    fun onSelectAudioDevice(device: AudioDevice) {
        callManager.selectAudioDevice(device)
    }

    /**
     * Retry the last outgoing call (used from the "Call Again" button on the failed screen).
     */
    fun onCallAgain() {
        viewModelScope.launch {
            callManager.callAgain()
        }
    }

    /**
     * Send ringing signal to caller (for incoming calls).
     */
    fun sendRinging() {
        callManager.sendRinging()
    }

    /**
     * Refresh the list of available audio devices.
     * Call this after Bluetooth permission is granted.
     */
    fun refreshAudioDevices() {
        callManager.refreshAudioDevices()
    }

    /**
     * Check if current state is the specified type.
     */
    inline fun <reified T : CallUiState> isState(): Boolean {
        return callUiState.value is T
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
}
