package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.ui.CallViewModel

/**
 * Main call screen that routes to the appropriate sub-screen based on call state.
 * All active call states (Outgoing, Connecting, Connected, Reconnecting) use a single
 * OngoingCallScreen composable, preventing remounts and UI blink on state transitions.
 */
@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onDismiss: () -> Unit
) {
    val callState by viewModel.callUiState.collectAsState()
    val mediaState by viewModel.mediaState.collectAsState()
    val duration by viewModel.formattedDuration.collectAsState()
    val remoteParticipant by viewModel.remoteParticipant.collectAsState()
    val availableDevices by viewModel.availableAudioDevices.collectAsState()
    val selectedDevice by viewModel.selectedAudioDevice.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = callState) {
            is CallUiState.Idle -> {
                // Should dismiss, handled by activity
            }

            is CallUiState.Incoming -> {
                IncomingCallScreen(
                    callerName = state.callerName ?: state.callerId,
                    callerAvatar = state.callerAvatar,
                    isVideo = state.isVideo,
                    onAnswer = { viewModel.onAnswerClick() },
                    onDecline = { viewModel.onDeclineClick() }
                )
            }

            is CallUiState.Outgoing,
            is CallUiState.Connecting,
            is CallUiState.Connected,
            is CallUiState.Reconnecting -> {
                OngoingCallScreen(
                    callState = state,
                    remoteName = when (state) {
                        is CallUiState.Outgoing -> state.remoteUserName ?: state.remoteUserId
                        else -> remoteParticipant?.name ?: ""
                    },
                    remoteAvatar = when (state) {
                        is CallUiState.Outgoing -> state.remoteUserAvatar
                        else -> remoteParticipant?.avatar
                    },
                    mediaState = mediaState,
                    duration = duration,
                    isRinging = remoteParticipant?.ringing ?: false,
                    audioDeviceData = AudioDeviceData(
                        availableDevices = availableDevices,
                        selectedDevice = selectedDevice
                    ),
                    onToggleMute = { viewModel.onToggleMute() },
                    onToggleCamera = { viewModel.onToggleCamera() },
                    onSwitchCamera = { viewModel.onSwitchCamera() },
                    onSelectDevice = { viewModel.onSelectAudioDevice(it) },
                    onEndCall = { viewModel.onEndCallClick() }
                )
            }

            is CallUiState.Ended -> {
                when (state) {
                    // Local hangup — close immediately, no screen shown
                    is CallUiState.Ended.LocalHangup -> {
                        LaunchedEffect(Unit) { onDismiss() }
                    }

                    // Remote hangup — show "Call Ended" briefly, then auto-close via Idle state
                    is CallUiState.Ended.RemoteHangup -> {
                        EndedCallScreen(
                            remoteName = remoteParticipant?.name ?: "Unknown",
                            remoteAvatar = remoteParticipant?.avatar,
                            reason = state.displayMessage,
                            onCancel = null,
                            onCallAgain = null
                        )
                    }

                    // Failed, declined, or no-answer — show the ended screen with actions
                    else -> {
                        EndedCallScreen(
                            remoteName = remoteParticipant?.name ?: "Unknown",
                            remoteAvatar = remoteParticipant?.avatar,
                            reason = state.displayMessage,
                            onCancel = onDismiss,
                            onCallAgain = { viewModel.onCallAgain() }
                        )
                    }
                }
            }
        }
    }
}
