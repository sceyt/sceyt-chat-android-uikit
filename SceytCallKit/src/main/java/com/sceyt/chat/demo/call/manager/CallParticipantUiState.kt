package com.sceyt.chat.demo.call.manager

import androidx.compose.runtime.Immutable
import com.callclient.call.data.ParticipantConnectionState
import com.sceyt.chat.models.signal.ParticipantState
import org.webrtc.VideoTrack

@Immutable
data class CallParticipantUiState(
    val userId: String,
    val clientId: String = "",
    val name: String? = null,
    val avatarUrl: String? = null,
    val isSelf: Boolean = false,
    val participantState: ParticipantState = ParticipantState.Idle,
    val connectionState: ParticipantConnectionState = ParticipantConnectionState.Idle,
    val isMuted: Boolean = false,
    val isVideoEnabled: Boolean = false,
    val videoTrack: VideoTrack? = null,
    val isActiveSpeaker: Boolean = false,
    val isFrontCamera: Boolean = true,
    val isOnHold: Boolean = false,
    val isScreenSharing: Boolean = false,
    val canPublishAudio: Boolean = true,
    val canPublishVideo: Boolean = true,
) {
    val displayName: String
        get() = name ?: userId

    val isConnected: Boolean
        get() = connectionState == ParticipantConnectionState.Connected

    val isRinging: Boolean
        get() = participantState == ParticipantState.Ringing

    val shouldShowLocalPreview: Boolean
        get() = isSelf && isVideoEnabled && videoTrack != null

    val isVisibleInGroupGrid: Boolean
        get() = if (isSelf) {
            true
        } else {
            participantState == ParticipantState.Joined
        }
}
