package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.chat.demo.call.ui.components.CallActionButton
import com.sceyt.chat.demo.call.ui.components.UserAvatar
import com.sceyt.chat.demo.call.ui.theme.CallColors

/**
 * Connecting screen - shown while establishing connection after answer.
 */
@Composable
fun ConnectingScreen(
    remoteName: String,
    remoteAvatar: String?,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "connecting")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
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
            Spacer(modifier = Modifier.height(100.dp))

            // Avatar with rotating progress ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Rotating progress ring
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(170.dp)
                        .rotate(rotation),
                    color = CallColors.AccentBlue,
                    strokeWidth = 3.dp,
                    trackColor = CallColors.SurfaceLight.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )

                // Avatar glow
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        CallColors.AccentBlue.copy(alpha = 0.2f),
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
                    size = 130.dp
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

            Spacer(modifier = Modifier.height(16.dp))

            // Connecting status
            ConnectingText()

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
private fun ConnectingText() {
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
        text = "Connecting$dots",
        color = CallColors.AccentBlue,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}
