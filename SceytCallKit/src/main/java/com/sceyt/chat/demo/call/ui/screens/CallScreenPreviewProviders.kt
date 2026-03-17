package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.callclient.call.data.ParticipantConnectionState
import com.sceyt.chat.demo.call.manager.CallParticipantUiState
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.MediaState

class IncomingCallIsVideoProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

data class EndedCallPreviewData(
    val remoteName: String,
    val remoteAvatar: String?,
    val reason: String,
    val showActions: Boolean
)

class EndedCallPreviewProvider : PreviewParameterProvider<EndedCallPreviewData> {
    override val values = sequenceOf(
        EndedCallPreviewData("Alice Johnson", null, "Call Declined", true),
        EndedCallPreviewData("Bob Smith", null, "No Answer", true),
        EndedCallPreviewData("Charlie Brown", null, "Call Ended", false),
        EndedCallPreviewData("Annual Meeting", null, "Call Failed", true),
    )
}

data class OngoingCallPreviewData(
    val callState: CallUiState,
    val mediaState: MediaState,
    val duration: String
)

class OngoingCallPreviewProvider : PreviewParameterProvider<OngoingCallPreviewData> {
    override val values = sequenceOf(
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Outgoing,
                participants = listOf(
                    CallParticipantUiState(userId = "me", name = "You", isSelf = true),
                    CallParticipantUiState(userId = "user1", name = "Alice Johnson")
                )
            ),
            mediaState = MediaState.DEFAULT_AUDIO,
            duration = ""
        ),
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Connected,
                participants = listOf(
                    CallParticipantUiState(userId = "me", name = "You", isSelf = true),
                    CallParticipantUiState(
                        userId = "user3",
                        name = "Charlie Brown",
                        connectionState = ParticipantConnectionState.Connected
                    )
                ),
                connectedAt = System.currentTimeMillis() - 65_000
            ),
            mediaState = MediaState.DEFAULT_AUDIO,
            duration = "01:05"
        )
    )
}
