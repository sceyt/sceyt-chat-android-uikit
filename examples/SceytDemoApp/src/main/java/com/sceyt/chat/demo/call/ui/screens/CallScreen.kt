package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.ui.CallViewModel

/**
 * Main call screen that routes to appropriate sub-screen based on call state.
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

            is CallUiState.Outgoing -> {
                OutgoingCallScreen(
                    remoteName = state.remoteUserName ?: state.remoteUserId,
                    remoteAvatar = state.remoteUserAvatar,
                    isVideo = state.isVideo,
                    onEndCall = { viewModel.onEndCallClick() }
                )
            }

            is CallUiState.Connecting -> {
                ConnectingScreen(
                    remoteName = remoteParticipant?.name ?: "Connecting...",
                    remoteAvatar = remoteParticipant?.avatar,
                    onEndCall = { viewModel.onEndCallClick() }
                )
            }

            is CallUiState.Connected -> {
                ConnectedCallScreen(
                    remoteName = remoteParticipant?.name ?: "Unknown",
                    remoteAvatar = remoteParticipant?.avatar,
                    duration = duration,
                    mediaState = mediaState,
                    availableDevices = availableDevices,
                    selectedDevice = selectedDevice,
                    onToggleMute = { viewModel.onToggleMute() },
                    onToggleCamera = { viewModel.onToggleCamera() },
                    onSwitchCamera = { viewModel.onSwitchCamera() },
                    onToggleSpeaker = { viewModel.onToggleSpeaker() },
                    onSelectDevice = { viewModel.onSelectAudioDevice(it) },
                    onEndCall = { viewModel.onEndCallClick() }
                )
            }

            is CallUiState.Reconnecting -> {
                ReconnectingScreen(
                    remoteName = remoteParticipant?.name ?: "Unknown",
                    attempt = state.attempt,
                    maxAttempts = state.maxAttempts,
                    isMuted = mediaState.isMuted,
                    onToggleMute = { viewModel.onToggleMute() },
                    onEndCall = { viewModel.onEndCallClick() }
                )
            }

            is CallUiState.Ended -> {
                EndedCallScreen(
                    reason = state.displayMessage,
                    onDismiss = onDismiss
                )
            }
        }
    }
}
