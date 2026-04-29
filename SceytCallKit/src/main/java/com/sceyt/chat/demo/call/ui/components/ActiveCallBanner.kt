package com.sceyt.chat.demo.call.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.sceyt.chat.demo.call.manager.CallParticipantUiState
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.displayTitle
import com.sceyt.chat.demo.call.manager.resolveStatusText
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private fun Modifier.bannerGradient(): Modifier = drawBehind {
    val angleRad = Math.toRadians(160.86).toFloat()
    val dx = sin(angleRad)
    val dy = -cos(angleRad)
    val len = size.width * abs(dx) + size.height * abs(dy)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val half = len / 2f
    drawRect(
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0.265f to Color(0xFF00C472).copy(alpha = 0.85f),
                1.000f to Color(0xFF5159F6).copy(alpha = 0.85f),
            ),
            start = Offset(cx - half * dx, cy - half * dy),
            end = Offset(cx + half * dx, cy + half * dy),
        ),
    )
}

@Composable
fun ActiveCallBanner(
    callState: CallUiState,
    duration: Long,
    onToggleMute: () -> Unit,
    onEndCall: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Start visible immediately if a call is already active (e.g. navigating back from CallActivity).
    // Delay only when a call becomes active while we're already on screen, so the banner doesn't
    // flash briefly in the background during CallActivity's opening animation.
    var showBanner by remember { mutableStateOf(callState.isActive) }
    LaunchedEffect(callState.isActive) {
        if (callState.isActive) {
            if (!showBanner) {
                delay(500)
                showBanner = true
            }
        } else {
            showBanner = false
        }
    }

    Box(modifier = modifier.fillMaxWidth().animateContentSize()) {
        if (showBanner) {
            val title = callState.call?.displayTitle(callState.remoteParticipants)
                ?: callState.remoteParticipant?.displayName
                ?: ""
            val statusText = callState.resolveStatusText(formatCallDuration(duration))
            val isMuted = callState.localParticipant?.isMuted == true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bannerGradient()
                    .clickable(
                        onClick = onClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp,
                    )
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (callState.phase != CallUiState.CallPhase.Incoming) {
                        BannerIconButton(onClick = onToggleMute) {
                            Icon(
                                imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    BannerIconButton(onClick = onEndCall) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.20f))
            .clickable(
                onClick = onClick,
                indication = ripple(bounded = true),
                interactionSource = remember { MutableInteractionSource() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun formatCallDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

private class ActiveCallBannerPreviewProvider : PreviewParameterProvider<CallUiState> {
    override val values = sequenceOf(
        CallUiState(
            phase = CallUiState.CallPhase.Outgoing,
            localParticipant = CallParticipantUiState(userId = "me", isSelf = true),
            remoteParticipants = listOf(CallParticipantUiState(userId = "u1", name = "Alice Johnson")),
        ),
        CallUiState(
            phase = CallUiState.CallPhase.Incoming,
            localParticipant = CallParticipantUiState(userId = "me", isSelf = true),
            remoteParticipants = listOf(CallParticipantUiState(userId = "u1", name = "Bob Smith")),
        ),
        CallUiState(
            phase = CallUiState.CallPhase.Connected,
            localParticipant = CallParticipantUiState(userId = "me", isSelf = true, isMuted = true),
            remoteParticipants = listOf(CallParticipantUiState(userId = "u1", name = "Charlie Brown")),
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF19191B)
@Composable
private fun ActiveCallBannerPreview(
    @PreviewParameter(ActiveCallBannerPreviewProvider::class) callState: CallUiState,
) {
    ActiveCallBanner(
        callState = callState,
        duration = 75L,
        onToggleMute = {},
        onEndCall = {},
        onClick = {},
    )
}
