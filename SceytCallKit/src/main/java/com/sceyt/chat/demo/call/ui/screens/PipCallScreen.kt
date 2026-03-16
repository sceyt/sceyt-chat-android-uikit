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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.CallUiState.CallPhase
import com.sceyt.chat.demo.call.manager.MediaState
import com.sceyt.chat.demo.call.ui.components.LocalVideoPreview
import com.sceyt.chat.demo.call.ui.components.RemoteVideoView
import com.sceyt.chat.demo.call.ui.components.UserAvatar
import com.sceyt.chat.demo.call.ui.theme.callBackground

internal val pipPhases = setOf(
    CallPhase.Incoming,
    CallPhase.Outgoing,
    CallPhase.Connecting,
    CallPhase.Connected,
    CallPhase.Reconnecting
)

/**
 * Compact call UI shown inside the PiP floating window.
 * Audio calls: dark call background + avatar + name + mute state + status.
 * Video calls: video stream(s) full-bleed; local preview overlaid bottom-right when both are active.
 */
@Composable
internal fun PipCallContent(
    callState: CallUiState,
    mediaState: MediaState,
    duration: String
) {
    val remoteName = callState.remoteUserName ?: callState.remoteUserId

    if (mediaState.hasActiveVideo) {
        // Video PiP — at least one video stream is active
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (mediaState.shouldShowRemoteVideo) {
                RemoteVideoView(
                    videoTrack = mediaState.remoteVideoTrack,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Only local video on — show it full screen
                LocalVideoPreview(
                    videoTrack = mediaState.localVideoTrack,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Local preview overlay — only when both streams are active
            if (mediaState.shouldShowRemoteVideo && mediaState.shouldShowLocalPreview) {
                val localPreviewShape = RoundedCornerShape(4.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth(0.43f)
                        .fillMaxHeight(0.40f)
                        .padding(bottom = 8.dp, end = 8.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), localPreviewShape)
                        .clip(localPreviewShape)
                ) {
                    LocalVideoPreview(
                        videoTrack = mediaState.localVideoTrack,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    } else {
        // Audio PiP — same dark call background, centered avatar + name + mute + status
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
                    avatarUrl = callState.remoteUserAvatar,
                    name = remoteName
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = remoteName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (mediaState.isRemoteMuted) {
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
}
