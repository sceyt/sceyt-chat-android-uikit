package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.chat.call.R
import com.sceyt.chat.demo.call.ui.CallViewModel
import org.koin.androidx.compose.koinViewModel

private val SettingsSheetBg = Color(0xFF232324)
private val SettingsHandleColor = Color(0xFF3B3B3D)
private val SettingsTitleColor = Color(0xFFE1E3E6)
private val SettingsSectionHeaderColor = Color(0xFF969A9F)
private val SettingsRowTextColor = Color(0xFFE1E3E6)
private val SettingsDividerColor = Color(0xFF3B3B3D)
private val DestructiveButtonBg = Color(0xFF2E2022)
private val DestructiveButtonText = Color(0xFFFA4C56)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CallSettingsBottomSheet(
    onDismiss: () -> Unit,
    viewModel: CallViewModel = koinViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val callState by viewModel.callUiState.collectAsState()
    val permissions = callState.callPermissions

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SettingsSheetBg,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(SettingsHandleColor),
            )
        },
    ) {
        Text(
            text = stringResource(R.string.call_settings),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = SettingsTitleColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionHeader(text = stringResource(R.string.call_permissions))

        PermissionToggleRow(
            label = stringResource(R.string.allow_audio),
            checked = permissions.allowPublishAudio,
            onCheckedChange = { enabled ->
                viewModel.onUpdateCallPermissions(
                    permissions.copy(allowPublishAudio = enabled)
                )
            },
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 16.dp)
                .background(SettingsDividerColor)
        )

        PermissionToggleRow(
            label = stringResource(R.string.allow_video),
            checked = permissions.allowPublishVideo,
            onCheckedChange = { enabled ->
                viewModel.onUpdateCallPermissions(
                    permissions.copy(allowPublishVideo = enabled)
                )
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionHeader(text = stringResource(R.string.participant_actions))

        SettingsActionButton(
            label = stringResource(R.string.mute_all_participants),
            enabled = permissions.allowPublishAudio,
            onClick = {
                viewModel.onMuteAllParticipants()
                onDismiss()
            },
        )

        Spacer(modifier = Modifier.height(4.dp))

        SettingsActionButton(
            label = stringResource(R.string.disable_all_video),
            enabled = permissions.allowPublishVideo,
            onClick = {
                viewModel.onDisableAllVideo()
                onDismiss()
            },
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = SettingsSectionHeaderColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun PermissionToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = SettingsRowTextColor,
            fontSize = 16.sp,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6B72FF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF3B3B3D),
            ),
        )
    }
}

@Composable
private fun SettingsActionButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DestructiveButtonBg,
                contentColor = DestructiveButtonText,
                disabledContainerColor = DestructiveButtonBg.copy(alpha = 0.4f),
                disabledContentColor = DestructiveButtonText.copy(alpha = 0.4f),
            ),
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
