package com.sceyt.chat.demo.call.manager

internal fun CallUiState.resolveStatusText(duration: String): String {
    if (call?.isGroupCall != true) {
        return when (phase) {
            CallUiState.CallPhase.Incoming -> "Incoming call"
            CallUiState.CallPhase.Outgoing -> if (isRemoteRinging) "ringing…" else "calling…"
            CallUiState.CallPhase.Connecting -> "connecting…"
            CallUiState.CallPhase.Connected -> duration
            CallUiState.CallPhase.Reconnecting -> "reconnecting…"
            else -> ""
        }
    }

    return when (phase) {
        CallUiState.CallPhase.Outgoing,
        CallUiState.CallPhase.Connecting -> "connecting…"

        CallUiState.CallPhase.Reconnecting -> "reconnecting…"
        CallUiState.CallPhase.Connected -> if (hasConnectedRemote) duration else "Waiting for others…"
        else -> ""
    }
}
