package com.sceyt.chat.demo.call.manager

import com.callclient.call.Call

/**
 * Application-level call state that maps SDK states to UI states.
 * A single data class with a [CallPhase] enum eliminates state loss when transitioning
 * between phases — user info (name, avatar, isVideo) is written once and survives all transitions.
 */
data class CallUiState(
    val phase: CallPhase = CallPhase.Idle,

    // Participant identity — set when call starts, persists through all phases
    val remoteUserId: String = "",
    val remoteUserName: String? = null,
    val remoteUserAvatar: String? = null,
    val isVideo: Boolean = false,

    // Incoming-phase only: the Call object needed to answer/reject
    val incomingCall: Call? = null,

    // Outgoing-phase: remote device has started ringing
    val isRemoteRinging: Boolean = false,

    // Connected / Reconnecting
    val connectedAt: Long = 0,
    val reconnectAttempt: Int = 0,
    val maxReconnectAttempts: Int = MAX_RECONNECT_ATTEMPTS,

    // Ended phase: non-null describes why the call ended
    val endedReason: EndedReason? = null,
) {

    enum class CallPhase {
        Idle, Incoming, Outgoing, Connecting, Connected, Reconnecting, Ended
    }

    sealed class EndedReason {
        data object LocalHangup : EndedReason()
        data object RemoteHangup : EndedReason()
        data class Declined(val reason: String? = null) : EndedReason()
        data object NoAnswer : EndedReason()
        data class Failed(val message: String) : EndedReason()

        val displayMessage: String
            get() = when (this) {
                is LocalHangup -> "Call Ended"
                is RemoteHangup -> "Call Ended"
                is Declined -> reason ?: "Call Declined"
                is NoAnswer -> "No Answer"
                is Failed -> message
            }

        val dismissTimeoutMs: Long
            get() = when (this) {
                is LocalHangup -> 1_000L
                is RemoteHangup -> 2_000L
                is Declined -> 10_000L
                is NoAnswer -> 3_000L
                is Failed -> 10_000L
            }
    }

    val isActive: Boolean
        get() = phase != CallPhase.Idle && phase != CallPhase.Ended

    val isRinging: Boolean
        get() = phase == CallPhase.Incoming || phase == CallPhase.Outgoing

    companion object {
        val IDLE = CallUiState()
        const val MAX_RECONNECT_ATTEMPTS = 3
        const val NO_ANSWER_TIMEOUT_MS = 60_000L
        const val RECONNECT_TIMEOUT_MS = 30_000L
    }
}
