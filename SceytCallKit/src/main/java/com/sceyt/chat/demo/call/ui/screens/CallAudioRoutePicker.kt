package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sceyt.audiorouting.AudioDevice
import com.sceyt.chat.demo.call.ui.components.AudioDeviceSelector

@Composable
internal fun CallAudioRoutePicker(
    availableDevices: List<AudioDevice>,
    selectedDevice: AudioDevice?,
    onToggleSpeaker: () -> Unit,
    onSelectDevice: (AudioDevice) -> Unit,
    content: @Composable (onAudioRouteClick: () -> Unit) -> Unit
) {
    var showAudioDeviceSelector by remember { mutableStateOf(false) }
    val shouldShowPicker = availableDevices.size > 2

    LaunchedEffect(shouldShowPicker, showAudioDeviceSelector) {
        if (showAudioDeviceSelector && !shouldShowPicker) {
            showAudioDeviceSelector = false
        }
    }

    val onAudioRouteClick = {
        if (shouldShowPicker) {
            showAudioDeviceSelector = true
        } else {
            onToggleSpeaker()
        }
    }

    content(onAudioRouteClick)

    if (showAudioDeviceSelector && shouldShowPicker) {
        AudioDeviceSelector(
            availableDevices = availableDevices,
            selectedDevice = selectedDevice,
            onDeviceSelected = onSelectDevice,
            onDismiss = { showAudioDeviceSelector = false }
        )
    }
}
