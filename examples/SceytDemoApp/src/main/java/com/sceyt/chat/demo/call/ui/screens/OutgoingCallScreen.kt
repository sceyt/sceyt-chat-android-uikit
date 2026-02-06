package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.chat.demo.call.ui.components.CallActionButton
import com.sceyt.chat.demo.call.ui.components.UserAvatar
import com.sceyt.chat.demo.call.ui.theme.CallColors

/**
 * Outgoing call screen - shown while waiting for remote to answer.
 */
@Composable
fun OutgoingCallScreen(
    remoteName: String,
    remoteAvatar: String?,
    isVideo: Boolean,
    onEndCall: () -> Unit
) {
    // Pulsing animation for the avatar ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CallColors.GradientStart,
                        CallColors.GradientMiddle,
                        CallColors.GradientEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Call type indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = CallColors.SurfaceLight.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                    contentDescription = null,
                    tint = CallColors.AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isVideo) "Video Call" else "Voice Call",
                    color = CallColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Avatar with animated pulsing ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Pulsing ring
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .background(
                            color = CallColors.AccentGreen,
                            shape = CircleShape
                        )
                )

                // Avatar glow
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        CallColors.AccentGreen.copy(alpha = 0.3f),
                                        Color.Transparent
                                    ),
                                    radius = size.width * 0.7f
                                )
                            )
                        }
                )

                UserAvatar(
                    avatarUrl = remoteAvatar,
                    name = remoteName,
                    size = 140.dp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Name
            Text(
                text = remoteName,
                color = CallColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Calling status with animated dots
            CallingText()

            Spacer(modifier = Modifier.weight(1f))

            // End call button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(12.dp, CircleShape)
                        .background(CallColors.AccentRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CallActionButton(
                        icon = Icons.Default.CallEnd,
                        backgroundColor = CallColors.AccentRed,
                        iconTint = Color.White,
                        contentDescription = "End Call",
                        onClick = onEndCall,
                        size = 72.dp,
                        iconSize = 32.dp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Cancel",
                    color = CallColors.TextSecondary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun CallingText() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotsCount by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots_count"
    )

    val dots = ".".repeat(dotsCount.toInt().coerceIn(0, 3))

    Text(
        text = "Calling$dots",
        color = CallColors.AccentGreen,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}
