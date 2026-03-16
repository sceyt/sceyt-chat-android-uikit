package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.chat.demo.call.ui.components.UserAvatarWithOuter
import com.sceyt.chat.demo.call.ui.theme.CallColors
import com.sceyt.chat.demo.call.ui.theme.callBackground

private val DeclineRed = CallColors.HangupRed
private val AcceptGreen = CallColors.CallAgainGreen

@Composable
fun IncomingCallScreen(
    callerName: String,
    callerAvatar: String?,
    isVideo: Boolean,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .callBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(0.15f))

        UserAvatarWithOuter(
            avatarUrl = callerAvatar,
            name = callerName
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Name
        Text(
            text = callerName,
            color = CallColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Call type subtitle
        Text(
            text = if (isVideo) "Incoming Video Call" else "Incoming Audio Call",
            color = CallColors.TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IncomingCallButton(
                label = "Decline",
                backgroundColor = DeclineRed,
                icon = {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "Decline",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                },
                onClick = onDecline
            )

            IncomingCallButton(
                label = "Accept",
                backgroundColor = AcceptGreen,
                icon = {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Accept",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                },
                onClick = onAnswer
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF19191B)
@Composable
private fun IncomingCallScreenPreview(
    @PreviewParameter(IncomingCallIsVideoProvider::class) isVideo: Boolean
) {
    IncomingCallScreen(
        callerName = "Alice Johnson",
        callerAvatar = null,
        isVideo = isVideo,
        onAnswer = {},
        onDecline = {}
    )
}

@Composable
private fun IncomingCallButton(
    label: String,
    backgroundColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = label,
            color = CallColors.TextSecondary,
            fontSize = 14.sp
        )
    }
}
