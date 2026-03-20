package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.chat.demo.call.manager.CallParticipantUiState
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.CallUiState.CallPhase
import com.sceyt.chat.demo.call.manager.displayTitle
import com.sceyt.chat.demo.call.manager.isGroupCall
import com.sceyt.chat.demo.call.ui.components.LocalVideoPreview
import com.sceyt.chat.demo.call.ui.components.RemoteVideoView
import com.sceyt.chat.demo.call.ui.components.StableVideoTrack
import com.sceyt.chat.demo.call.ui.components.UserAvatar
import com.sceyt.chat.demo.call.ui.theme.callBackground

internal val pipPhases = setOf(
    CallPhase.Incoming,
    CallPhase.Outgoing,
    CallPhase.Connecting,
    CallPhase.Connected,
    CallPhase.Reconnecting
)

@Composable
internal fun PipCallContent(
    callState: CallUiState,
    duration: String
) {
    // Group call PIP: show only self video
    if (callState.call?.isGroupCall == true) {
        val stableLocalTrack = remember(callState.localParticipant?.videoTrack) {
            StableVideoTrack(callState.localParticipant?.videoTrack)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            LocalVideoPreview(
                videoTrack = stableLocalTrack,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    // P2P call PIP: show remote video (with local overlay), or audio-only fallback
    val focusParticipant = rememberPipFocusParticipant(callState)
    val displayTitle = callState.call?.displayTitle(callState.remoteParticipants)
        ?: focusParticipant?.displayName.orEmpty()
    val hasRemoteVideo = focusParticipant?.videoTrack != null && focusParticipant.isVideoEnabled
    val hasLocalVideo = callState.localParticipant?.shouldShowLocalPreview == true
    val stableRemoteTrack = remember(focusParticipant?.videoTrack) {
        StableVideoTrack(focusParticipant?.videoTrack)
    }
    val stableLocalTrack = remember(callState.localParticipant?.videoTrack) {
        StableVideoTrack(callState.localParticipant?.videoTrack)
    }

    if (hasRemoteVideo || hasLocalVideo) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (hasRemoteVideo) {
                RemoteVideoView(
                    videoTrack = stableRemoteTrack,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LocalVideoPreview(
                    videoTrack = stableLocalTrack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (hasRemoteVideo && hasLocalVideo) {
                val shape = RoundedCornerShape(4.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth(0.43f)
                        .fillMaxHeight(0.40f)
                        .padding(bottom = 8.dp, end = 8.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
                        .clip(shape)
                ) {
                    LocalVideoPreview(
                        videoTrack = stableLocalTrack,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .callBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            UserAvatar(
                modifier = Modifier.size(48.dp),
                avatarUrl = focusParticipant?.avatarUrl,
                name = focusParticipant?.displayName ?: displayTitle
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayTitle,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (focusParticipant?.isMuted == true) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Muted",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            CallStatusContent(
                callState = callState,
                duration = duration,
                fontSize = 11.sp
            )
        }
    }
}

private fun rememberPipFocusParticipant(callState: CallUiState): CallParticipantUiState? {
    return callState.remoteParticipants
        .firstOrNull { it.isActiveSpeaker && it.videoTrack != null }
        ?: callState.remoteParticipants.firstOrNull { it.isConnected && it.videoTrack != null }
        ?: callState.remoteParticipants.firstOrNull { it.videoTrack != null }
        ?: callState.remoteParticipant
}
