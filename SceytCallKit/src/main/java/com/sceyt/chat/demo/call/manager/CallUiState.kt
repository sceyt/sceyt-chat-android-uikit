package com.sceyt.chat.demo.call.manager

import androidx.compose.runtime.Immutable
import com.callclient.call.Call

/**
 * Application-level call state shared by all call screens.
 */
@Immutable
data class CallUiState(
    val phase: CallPhase = CallPhase.Idle,
    val call: Call? = null,
    val localParticipant: CallParticipantUiState? = null,
    val remoteParticipants: List<CallParticipantUiState> = emptyList(),
    val isRemoteRinging: Boolean = false,
    val connectedAt: Long = 0,
    val endedReason: EndedReason? = null,
) {

    enum class CallPhase {
        Idle, Incoming, Outgoing, Connecting, Connected, Reconnecting, Ended;

        fun canAnswerOrMakeCall(): Boolean {
            return this == Idle || this == Ended
        }
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
                is LocalHangup -> 0L
                is RemoteHangup -> 2_000L
                is Declined -> 10_000L
                is NoAnswer -> 3_000L
                is Failed -> 10_000L
            }
    }

    val remoteParticipant: CallParticipantUiState?
        get() = remoteParticipants.firstOrNull()

    val connectedRemoteCount: Int
        get() = remoteParticipants.count { it.isConnected }

    val hasConnectedRemote: Boolean
        get() = connectedRemoteCount > 0

    val shouldShowRunningTimer: Boolean
        get() = phase == CallPhase.Connected && hasConnectedRemote

    val isActive: Boolean
        get() = phase != CallPhase.Idle && phase != CallPhase.Ended

    companion object {
        val IDLE = CallUiState()
        const val NO_ANSWER_TIMEOUT_MS = 60_000L
        const val RECONNECT_TIMEOUT_MS = 60_000L
    }
}
