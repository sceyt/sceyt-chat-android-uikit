package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.chat.call.R
import com.sceyt.chat.demo.call.ui.CallMemberUiState
import com.sceyt.chat.demo.call.ui.CallMembersViewModel

private val ControlSheetBg = Color(0xFF232324)
private val ControlHandleColor = Color(0xFF3B3B3D)
private val ControlNameColor = Color(0xFFE1E3E6)
private val ControlRowTextColor = Color(0xFFE1E3E6)
private val ControlIconBg = Color(0xFF303032)
private val ControlDividerColor = Color(0xFF3B3B3D)
private val DestructiveColor = Color(0xFFFA4C56)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParticipantControlBottomSheet(
    member: CallMemberUiState,
    onDismiss: () -> Unit,
    viewModel: CallMembersViewModel,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val participant = member.participant
    val userId = participant.userId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ControlSheetBg,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(ControlHandleColor),
            )
        },
    ) {
        Text(
            text = participant.displayName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = ControlNameColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Audio controls ----
        if (!participant.isMuted) {
            ControlRow(
                icon = Icons.Default.MicOff,
                label = stringResource(R.string.mute),
                iconTint = DestructiveColor,
                onClick = {
                    viewModel.onMuteParticipant(userId)
                    onDismiss()
                },
            )
        } else if (participant.canPublishAudio) {
            ControlRow(
                icon = Icons.Default.Mic,
                label = stringResource(R.string.mute),
                onClick = {
                    viewModel.onUnmuteParticipant(userId)
                    onDismiss()
                },
            )
        }

        if (participant.canPublishAudio) {
            ControlDivider()
            ControlRow(
                icon = Icons.Default.Lock,
                label = stringResource(R.string.lock_audio),
                iconTint = DestructiveColor,
                onClick = {
                    viewModel.onLockParticipantAudio(userId)
                    onDismiss()
                },
            )
        } else {
            ControlDivider()
            ControlRow(
                icon = Icons.Default.LockOpen,
                label = stringResource(R.string.unlock_audio),
                onClick = {
                    viewModel.onUnlockParticipantAudio(userId)
                    onDismiss()
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---- Video controls ----
        if (participant.isVideoEnabled) {
            ControlRow(
                icon = Icons.Default.VideocamOff,
                label = stringResource(R.string.disable_video),
                iconTint = DestructiveColor,
                onClick = {
                    viewModel.onDisableParticipantVideo(userId)
                    onDismiss()
                },
            )
        } else if (participant.canPublishVideo) {
            ControlRow(
                icon = Icons.Default.Videocam,
                label = stringResource(R.string.enable_video),
                onClick = {
                    viewModel.onEnableParticipantVideo(userId)
                    onDismiss()
                },
            )
        }

        if (participant.canPublishVideo) {
            ControlDivider()
            ControlRow(
                icon = Icons.Default.Lock,
                label = stringResource(R.string.lock_video),
                iconTint = DestructiveColor,
                onClick = {
                    viewModel.onLockParticipantVideo(userId)
                    onDismiss()
                },
            )
        } else {
            ControlDivider()
            ControlRow(
                icon = Icons.Default.LockOpen,
                label = stringResource(R.string.unlock_video),
                onClick = {
                    viewModel.onUnlockParticipantVideo(userId)
                    onDismiss()
                },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ControlRow(
    icon: ImageVector,
    label: String,
    iconTint: Color = ControlRowTextColor,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ControlIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = ControlRowTextColor,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun ControlDivider() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ControlDividerColor)
        )
    }
}
