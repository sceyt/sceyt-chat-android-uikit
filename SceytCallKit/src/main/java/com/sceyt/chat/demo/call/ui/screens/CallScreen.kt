package com.sceyt.chat.demo.call.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.sceyt.chat.call.R
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.CallUiState.CallPhase
import com.sceyt.chat.demo.call.manager.displayTitle
import com.sceyt.chat.demo.call.manager.isGroupCall
import com.sceyt.chat.demo.call.manager.isVideoCall
import com.sceyt.chat.demo.call.ui.CallViewModel

/**
 * Main call screen that routes to the appropriate sub-screen based on call phase.
 * All active phases (Outgoing, Connecting, Connected, Reconnecting) use a single
 * OngoingCallScreen composable, preventing remounts and UI blink on phase transitions.
 * Call-level display data is derived from the active [com.callclient.call.Call] when available.
 */
@Composable
fun CallScreen(
    viewModel: CallViewModel,
    isInPipMode: Boolean = false,
    onDismiss: () -> Unit
) {
    val callState by viewModel.callUiState.collectAsState()
    val mediaState by viewModel.mediaState.collectAsState()
    val duration by viewModel.formattedDuration.collectAsState()
    val availableDevices by viewModel.availableAudioDevices.collectAsState()
    val selectedDevice by viewModel.selectedAudioDevice.collectAsState()

    val context = LocalContext.current

    fun hasMicPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.onToggleCamera()
    }
    val onToggleCameraWithPermission: () -> Unit = {
        if (mediaState.isCameraEnabled) {
            viewModel.onToggleCamera()  // turning off — no permission needed
        } else if (hasCameraPermission()) {
            viewModel.onToggleCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Mic is required for all calls; camera additionally required for video calls.
    // Answer is only proceeded if microphone is granted.
    val answerPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.onAnswerClick()
        }
    }
    val onAnswerWithPermission: () -> Unit = {
        val needsMic = !hasMicPermission()
        val needsCamera = callState.call?.isVideoCall == true && !hasCameraPermission()
        if (needsMic || needsCamera) {
            val permissions = buildList {
                if (needsMic) add(Manifest.permission.RECORD_AUDIO)
                if (needsCamera) add(Manifest.permission.CAMERA)
            }.toTypedArray()
            answerPermissionsLauncher.launch(permissions)
        } else {
            viewModel.onAnswerClick()
        }
    }

    // PiP mode handling
    if (isInPipMode) {
        when {
            callState.phase in pipPhases ->
                PipCallContent(callState = callState, mediaState = mediaState, duration = duration)
            // Call ended/idle in PiP — close immediately (EndedCallScreen buttons are unusable in PiP)
            else -> LaunchedEffect(Unit) { onDismiss() }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val call = callState.call
        val displayTitle = call?.displayTitle(callState.participants)
            ?: callState.remoteParticipant?.displayName.orEmpty()
        val isGroupCall = call?.isGroupCall == true
        val isVideoCall = call?.isVideoCall == true

        when (callState.phase) {
            CallPhase.Idle -> {
                // Auto-close handled by CallActivity
            }

            CallPhase.Incoming -> {
                IncomingCallScreen(
                    callerName = displayTitle,
                    callerAvatar = if (isGroupCall) null else callState.remoteParticipant?.avatarUrl,
                    isVideo = isVideoCall,
                    onAnswer = onAnswerWithPermission,
                    onDecline = viewModel::onDeclineClick
                )
            }

            CallPhase.Outgoing,
            CallPhase.Connecting,
            CallPhase.Connected,
            CallPhase.Reconnecting -> {
                OngoingCallScreen(
                    callState = callState,
                    mediaState = mediaState,
                    duration = duration,
                    audioDeviceData = AudioDeviceData(availableDevices, selectedDevice),
                    onToggleMute = viewModel::onToggleMute,
                    onToggleCamera = onToggleCameraWithPermission,
                    onSwitchCamera = viewModel::onSwitchCamera,
                    onSelectDevice = viewModel::onSelectAudioDevice,
                    onEndCall = viewModel::onEndCallClick
                )
            }

            CallPhase.Ended -> {
                when (val reason = callState.endedReason) {
                    // Local hangup — close immediately, no screen shown
                    is CallUiState.EndedReason.LocalHangup -> {
                        LaunchedEffect(Unit) { onDismiss() }
                    }

                    // Remote hangup — show "Call Ended" briefly, then auto-close via Idle
                    is CallUiState.EndedReason.RemoteHangup -> {
                        EndedCallScreen(
                            remoteName = displayTitle,
                            remoteAvatar = if (isGroupCall) null else callState.remoteParticipant?.avatarUrl,
                            reason = reason.displayMessage,
                            onCancel = null,
                            onCallAgain = null
                        )
                    }

                    // Failed, declined, or no-answer — show ended screen with actions
                    else -> {
                        EndedCallScreen(
                            remoteName = displayTitle,
                            remoteAvatar = if (isGroupCall) null else callState.remoteParticipant?.avatarUrl,
                            reason = reason?.displayMessage ?: stringResource(R.string.call_ended),
                            onCancel = onDismiss,
                            onCallAgain = viewModel::onCallAgain
                        )
                    }
                }
            }
        }
    }
}
