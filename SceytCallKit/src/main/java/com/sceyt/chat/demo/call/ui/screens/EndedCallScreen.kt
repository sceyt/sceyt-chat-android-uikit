package com.sceyt.chat.demo.call.ui.screens

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.chat.demo.call.ui.components.CallActionButton
import com.sceyt.chat.demo.call.ui.components.UserAvatarWithOuter
import com.sceyt.chat.demo.call.ui.theme.CallColors
import com.sceyt.chat.demo.call.ui.theme.callBackground

/**
 * Screen shown when a call fails, is declined, or gets no answer.
 * Matches Figma design: avatar + name + status + Cancel/Call Again buttons.
 * NOT shown for normal hangup (LocalHangup/RemoteHangup) — activity closes directly for those.
 */
@Composable
fun EndedCallScreen(
    remoteName: String,
    remoteAvatar: String?,
    reason: String,
    onCancel: (() -> Unit)?,
    onCallAgain: (() -> Unit)?
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Box(
        modifier = Modifier
            .fillMaxSize()
            .callBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.fillMaxHeight(0.15f))

            // Avatar with outer ring
            UserAvatarWithOuter(
                avatarUrl = remoteAvatar,
                name = remoteName
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Name
            Text(
                text = remoteName,
                color = CallColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Status pill (e.g. "Call Failed", "Call Declined", "No Answer")
            Box(
                modifier = Modifier
                    .background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = reason,
                    color = CallColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Cancel and Call Again buttons (hidden for RemoteHangup)
            if (onCancel != null || onCallAgain != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cancel button
                    if (onCancel != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CallActionButton(
                                icon = Icons.Default.Close,
                                backgroundColor = CallColors.ButtonSurface,
                                iconTint = Color.White,
                                contentDescription = "Cancel",
                                onClick = onCancel,
                                size = 64.dp,
                                iconSize = 34.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cancel",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Call Again button
                    if (onCallAgain != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CallActionButton(
                                icon = Icons.Default.Phone,
                                backgroundColor = CallColors.CallAgainGreen,
                                iconTint = Color.White,
                                contentDescription = "Call Again",
                                onClick = onCallAgain,
                                size = 64.dp,
                                iconSize = 34.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Call Again",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        IconButton(
            onClick = { backDispatcher?.onBackPressed() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF19191B)
@Composable
private fun EndedCallScreenPreview(
    @PreviewParameter(EndedCallPreviewProvider::class) data: EndedCallPreviewData
) {
    EndedCallScreen(
        remoteName = data.remoteName,
        remoteAvatar = data.remoteAvatar,
        reason = data.reason,
        onCancel = if (data.showActions) ({}) else null,
        onCallAgain = if (data.showActions) ({}) else null
    )
}
