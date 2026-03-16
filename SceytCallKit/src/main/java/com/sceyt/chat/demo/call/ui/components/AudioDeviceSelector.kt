package com.sceyt.chat.demo.call.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sceyt.audiorouting.AudioDevice

private val DialogBg = Color(0xFF232324)
private val ItemTextColor = Color(0xFFE1E3E6)
private val IconAccentColor = Color(0xFF4F8CFF)
private val DividerColor = Color(0xFF3A3A3C)

/**
 * Bottom-anchored dialog for selecting audio output device.
 * Uses Compose Dialog for built-in fade animation and automatic
 * back-press / outside-tap dismissal.
 */
@Composable
fun AudioDeviceSelector(
    availableDevices: List<AudioDevice>,
    selectedDevice: AudioDevice?,
    onDeviceSelected: (AudioDevice) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DialogBg)
                    .navigationBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                availableDevices.forEachIndexed { index, device ->
                    AudioDeviceItem(
                        device = device,
                        isSelected = device.id == selectedDevice?.id,
                        onClick = {
                            onDeviceSelected(device)
                            onDismiss()
                        }
                    )
                    if (index < availableDevices.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = DividerColor,
                            thickness = 0.5.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AudioDeviceItem(
    device: AudioDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = device.icon,
            contentDescription = null,
            tint = if (isSelected) IconAccentColor else ItemTextColor.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = device.label,
            fontSize = 16.sp,
            color = if (isSelected) Color.White else ItemTextColor
        )
    }
}

private val AudioDevice.icon: ImageVector
    get() = when (this) {
        is AudioDevice.BluetoothHeadset -> Icons.Default.BluetoothAudio
        is AudioDevice.WiredHeadset -> Icons.Default.Headset
        is AudioDevice.Earpiece -> Icons.Default.Phone
        is AudioDevice.Speakerphone -> Icons.AutoMirrored.Filled.VolumeUp
    }

private val AudioDevice.label: String
    get() = when (this) {
        is AudioDevice.BluetoothHeadset -> name.ifBlank { "Bluetooth" }
        is AudioDevice.WiredHeadset -> "Wired Headset"
        is AudioDevice.Earpiece -> "Phone"
        is AudioDevice.Speakerphone -> "Speaker"
    }
