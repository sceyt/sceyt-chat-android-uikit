package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.callclient.call.data.ParticipantConnectionState
import com.sceyt.chat.demo.call.manager.CallParticipantUiState
import com.sceyt.chat.demo.call.manager.CallUiState

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
    val duration: String
)

class OngoingCallPreviewProvider : PreviewParameterProvider<OngoingCallPreviewData> {
    override val values = sequenceOf(
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Outgoing,
                localParticipant = CallParticipantUiState(userId = "me", name = "You", isSelf = true),
                remoteParticipants = listOf(
                    CallParticipantUiState(userId = "user1", name = "Alice Johnson")
                )
            ),
            duration = ""
        ),
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Connected,
                localParticipant = CallParticipantUiState(userId = "me", name = "You", isSelf = true),
                remoteParticipants = listOf(
                    CallParticipantUiState(
                        userId = "user3",
                        name = "Charlie Brown",
                        connectionState = ParticipantConnectionState.Connected
                    )
                ),
                connectedAt = System.currentTimeMillis() - 65_000
            ),
            duration = "01:05"
        )
    )
}

class GroupOngoingCallPreviewProvider : PreviewParameterProvider<OngoingCallPreviewData> {
    override val values = sequenceOf(
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Connected,
                localParticipant = CallParticipantUiState(
                    userId = "me",
                    name = "You",
                    isSelf = true,
                    isMuted = true,
                    isVideoEnabled = true
                ),
                remoteParticipants = listOf(
                    CallParticipantUiState(
                        userId = "user1",
                        name = "Alice Johnson",
                        connectionState = ParticipantConnectionState.Connected,
                        isActiveSpeaker = true
                    ),
                    CallParticipantUiState(
                        userId = "user2",
                        name = "Bob Smith",
                        connectionState = ParticipantConnectionState.Connected,
                        isMuted = true
                    ),
                    CallParticipantUiState(
                        userId = "user3",
                        name = "Charlie Brown",
                        connectionState = ParticipantConnectionState.Connected
                    ),
                    CallParticipantUiState(
                        userId = "user4",
                        name = "Diana Miller",
                        connectionState = ParticipantConnectionState.Connected
                    ),
                    CallParticipantUiState(
                        userId = "user5",
                        name = "Ethan Wilson",
                        connectionState = ParticipantConnectionState.Connected,
                        isMuted = true
                    )
                ),
                connectedAt = System.currentTimeMillis() - 8 * 60_000
            ),
            duration = "08:00"
        ),
        OngoingCallPreviewData(
            callState = CallUiState(
                phase = CallUiState.CallPhase.Reconnecting,
                localParticipant = CallParticipantUiState(userId = "me", name = "You", isSelf = true),
                remoteParticipants = listOf(
                    CallParticipantUiState(
                        userId = "user1",
                        name = "Alice Johnson",
                        connectionState = ParticipantConnectionState.Connected
                    ),
                    CallParticipantUiState(
                        userId = "user2",
                        name = "Bob Smith",
                        connectionState = ParticipantConnectionState.Connected
                    ),
                    CallParticipantUiState(
                        userId = "user3",
                        name = "Charlie Brown",
                        connectionState = ParticipantConnectionState.Connected
                    ),
                    CallParticipantUiState(
                        userId = "user4",
                        name = "Diana Miller",
                        connectionState = ParticipantConnectionState.Connected
                    ),
                    CallParticipantUiState(
                        userId = "user5",
                        name = "Ethan Wilson",
                        connectionState = ParticipantConnectionState.Connected
                    ),
                    CallParticipantUiState(
                        userId = "user6",
                        name = "Fiona Davis",
                        connectionState = ParticipantConnectionState.Connected
                    ),
                    CallParticipantUiState(
                        userId = "user7",
                        name = "George Martin",
                        connectionState = ParticipantConnectionState.Connected
                    ),
                    CallParticipantUiState(
                        userId = "user8",
                        name = "Hannah Clark",
                        connectionState = ParticipantConnectionState.Connected
                    ),
                    CallParticipantUiState(
                        userId = "user9",
                        name = "Ian Lewis",
                        connectionState = ParticipantConnectionState.Connected
                    )
                )
            ),
            duration = "12:34"
        )
    )
}

class ParticipantTilePreviewProvider : PreviewParameterProvider<CallParticipantUiState> {
    override val values = sequenceOf(
        CallParticipantUiState(
            userId = "me",
            name = "You",
            isSelf = true,
            connectionState = ParticipantConnectionState.Connected
        ),
        CallParticipantUiState(
            userId = "user1",
            name = "Alice Johnson",
            connectionState = ParticipantConnectionState.Connected,
            isActiveSpeaker = true
        ),
        CallParticipantUiState(
            userId = "user2",
            name = "Bob Smith",
            connectionState = ParticipantConnectionState.Connected,
            isMuted = true
        )
    )
}
