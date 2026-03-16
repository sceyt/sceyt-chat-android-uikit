package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.MediaState
import org.webrtc.VideoTrack

// ── IncomingCallScreen ────────────────────────────────────────────────────────

class IncomingCallIsVideoProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

// ── EndedCallScreen ───────────────────────────────────────────────────────────

data class EndedCallPreviewData(
    val remoteName: String,
    val remoteAvatar: String?,
    val reason: String,
    val showActions: Boolean
)

class EndedCallPreviewProvider : PreviewParameterProvider<EndedCallPreviewData> {
    override val values = sequenceOf(
        EndedCallPreviewData(
            remoteName = "Alice Johnson",
            remoteAvatar = null,
            reason = "Call Declined",
            showActions = true
        ),
        EndedCallPreviewData(
            remoteName = "Bob Smith",
            remoteAvatar = null,
            reason = "No Answer",
            showActions = true
        ),
        EndedCallPreviewData(
            remoteName = "Charlie Brown",
            remoteAvatar = null,
            reason = "Call Ended",
            showActions = false
        ),
        EndedCallPreviewData(
            remoteName = "Diana Prince",
            remoteAvatar = null,
            reason = "Call Failed",
            showActions = true
        ),
    )
}

// ── OngoingCallScreen ─────────────────────────────────────────────────────────

data class OngoingCallPreviewData(
    val callState: CallUiState,
    val mediaState: MediaState,
    val duration: String
)

class OngoingCallPreviewProvider : PreviewParameterProvider<OngoingCallPreviewData> {
    override val values = sequenceOf(
        // Outgoing audio call
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Outgoing,
                remoteUserId = "user1",
                remoteUserName = "Alice Johnson",
                isVideo = false
            ),
            mediaState = MediaState.DEFAULT_AUDIO,
            duration = ""
        ),
        // Connecting
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Connecting,
                remoteUserId = "user2",
                remoteUserName = "Bob Smith",
                isVideo = false
            ),
            mediaState = MediaState.DEFAULT_AUDIO,
            duration = ""
        ),
        // Connected audio
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Connected,
                remoteUserId = "user3",
                remoteUserName = "Charlie Brown",
                isVideo = false,
                connectedAt = System.currentTimeMillis() - 65_000
            ),
            mediaState = MediaState.DEFAULT_AUDIO,
            duration = "01:05"
        ),
        // Reconnecting
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Reconnecting,
                remoteUserId = "user4",
                remoteUserName = "Diana Prince",
                isVideo = false
            ),
            mediaState = MediaState.DEFAULT_AUDIO,
            duration = "02:30"
        ),

        // Connected video
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Connected,
                remoteUserId = "user5",
                remoteUserName = "Eve Adams",
                isVideo = true,
                connectedAt = System.currentTimeMillis() - 125_000
            ),
            mediaState = MediaState.DEFAULT_VIDEO.copy(
                localVideoTrack = VideoTrack(1)
            ),
            duration = "02:05"
        )
    )
}
