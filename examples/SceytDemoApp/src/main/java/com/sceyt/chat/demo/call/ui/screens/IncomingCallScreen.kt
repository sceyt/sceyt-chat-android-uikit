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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
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
 * Full-screen incoming call UI with modern design.
 */
@Composable
fun IncomingCallScreen(
    callerName: String,
    callerAvatar: String?,
    isVideo: Boolean,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    // Pulsing animation for incoming call
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
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
            Spacer(modifier = Modifier.height(60.dp))

            // Call type indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = CallColors.AccentBlue.copy(alpha = 0.15f),
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
                    text = if (isVideo) "Incoming Video Call" else "Incoming Voice Call",
                    color = CallColors.AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(50.dp))

            // Avatar with animated pulsing rings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Outer pulsing ring
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .background(
                            color = CallColors.AccentBlue,
                            shape = CircleShape
                        )
                )

                // Inner pulsing ring
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulseScale * 0.95f)
                        .alpha(pulseAlpha * 1.5f)
                        .background(
                            color = CallColors.AccentBlue,
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
                                        CallColors.AccentBlue.copy(alpha = 0.4f),
                                        Color.Transparent
                                    ),
                                    radius = size.width * 0.7f
                                )
                            )
                        }
                )

                UserAvatar(
                    avatarUrl = callerAvatar,
                    name = callerName,
                    size = 140.dp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Caller name
            Text(
                text = callerName,
                color = CallColors.TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ringing indicator
            RingingText()

            Spacer(modifier = Modifier.weight(1f))

            // Answer/Decline buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Decline button
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
                            contentDescription = "Decline",
                            onClick = onDecline,
                            size = 72.dp,
                            iconSize = 32.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Decline",
                        color = CallColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }

                // Answer button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(12.dp, CircleShape)
                            .background(CallColors.AccentGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CallActionButton(
                            icon = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            backgroundColor = CallColors.AccentGreen,
                            iconTint = Color.White,
                            contentDescription = "Answer",
                            onClick = onAnswer,
                            size = 72.dp,
                            iconSize = 32.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Answer",
                        color = CallColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun RingingText() {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )

    Text(
        text = "Ringing...",
        color = CallColors.TextSecondary.copy(alpha = alpha),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}
