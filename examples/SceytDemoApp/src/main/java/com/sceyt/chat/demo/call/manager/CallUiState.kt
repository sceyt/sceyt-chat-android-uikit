package com.sceyt.chat.demo.call.manager

import com.callclient.call.Call

/**
 * Application-level call state that maps SDK states to UI states.
 * This provides a unified view of the call lifecycle for the UI layer.
 */
sealed class CallUiState {

    /**
     * No active call.
     */
    data object Idle : CallUiState()

    /**
     * Outgoing call initiated - waiting for remote to answer.
     * Ringback tone should be played in this state.
     */
    data class Outgoing(
        val remoteUserId: String,
        val remoteUserName: String?,
        val remoteUserAvatar: String?,
        val isVideo: Boolean,
        val startTime: Long = System.currentTimeMillis()
    ) : CallUiState()

    /**
     * Incoming call received - ringing.
     * Ringtone should be played in this state.
     */
    data class Incoming(
        val callerId: String,
        val callerName: String?,
        val callerAvatar: String?,
        val isVideo: Boolean,
        val call: Call
    ) : CallUiState()

    /**
     * Call is being established after answer.
     * Transition state between Incoming/Outgoing and Connected.
     */
    data object Connecting : CallUiState()

    /**
     * Active call with media flowing.
     * Call duration timer should run in this state.
     */
    data class Connected(
        val connectedAt: Long = System.currentTimeMillis()
    ) : CallUiState()

    /**
     * Connection temporarily lost, attempting to reconnect.
     * Reconnecting tone should be played in this state.
     */
    data class Reconnecting(
        val attempt: Int = 1,
        val lastConnectedAt: Long,
        val maxAttempts: Int = MAX_RECONNECT_ATTEMPTS
    ) : CallUiState()

    /**
     * Terminal states - call has ended for various reasons.
     * Each has different UI timeout before dismissing.
     */
    sealed class Ended : CallUiState() {
        /** Local user hung up */
        data object LocalHangup : Ended()

        /** Remote participant hung up */
        data object RemoteHangup : Ended()

        /** Remote participant declined the call */
        data class Declined(val reason: String? = null) : Ended()

        /** Remote participant did not answer within timeout */
        data object NoAnswer : Ended()

        /** Call failed due to error */
        data class Failed(val reason: String) : Ended()

        /** Get display message for the ended state */
        val displayMessage: String
            get() = when (this) {
                is LocalHangup -> "Call Ended"
                is RemoteHangup -> "Call Ended"
                is Declined -> reason ?: "Call Declined"
                is NoAnswer -> "No Answer"
                is Failed -> reason
            }

        /** Get UI dismiss timeout in milliseconds */
        val dismissTimeoutMs: Long
            get() = when (this) {
                is LocalHangup -> 1_000L
                is RemoteHangup -> 2_000L
                is Declined -> 10_000L
                is NoAnswer -> 3_000L
                is Failed -> 10_000L
            }
    }

    companion object {
        const val MAX_RECONNECT_ATTEMPTS = 3
        const val NO_ANSWER_TIMEOUT_MS = 60_000L
        const val RECONNECT_TIMEOUT_MS = 30_000L
    }
}

/**
 * Extension to check if call is in an active state (not idle or ended).
 */
val CallUiState.isActive: Boolean
    get() = this !is CallUiState.Idle && this !is CallUiState.Ended

/**
 * Extension to check if call is in a ringing state (incoming or outgoing).
 */
val CallUiState.isRinging: Boolean
    get() = this is CallUiState.Incoming || this is CallUiState.Outgoing
