package com.sceyt.chat.demo.call.ui

import com.sceyt.audiorouting.AudioDevice
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.isVideoCall

internal fun shouldEnableProximityWakeLock(
    callState: CallUiState,
    selectedAudioDevice: AudioDevice?,
    isInPictureInPictureMode: Boolean,
): Boolean {
    if (isInPictureInPictureMode) return false
    if (callState.phase !in PROXIMITY_PHASES) return false
    if (selectedAudioDevice !is AudioDevice.Earpiece) return false

    val call = callState.call ?: return false
    if (call.isVideoCall) return false
    if (callState.localParticipant?.isVideoEnabled == true) return false
    if (callState.remoteParticipants.any { it.isVideoEnabled }) return false

    return true
}

private val PROXIMITY_PHASES = setOf(
    CallUiState.CallPhase.Connecting,
    CallUiState.CallPhase.Connected,
    CallUiState.CallPhase.Reconnecting,
)
