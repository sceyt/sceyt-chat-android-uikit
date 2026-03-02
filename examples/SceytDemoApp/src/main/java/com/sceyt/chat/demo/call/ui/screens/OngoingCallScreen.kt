package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.audiorouting.AudioDevice
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.MediaState
import com.sceyt.chat.demo.call.ui.components.AudioDeviceSelector
import com.sceyt.chat.demo.call.ui.components.CallActionButton
import com.sceyt.chat.demo.call.ui.components.LocalVideoPreview
import com.sceyt.chat.demo.call.ui.components.RemoteVideoView
import com.sceyt.chat.demo.call.ui.components.UserAvatar
import com.sceyt.chat.demo.call.ui.theme.CallColors
import kotlinx.coroutines.delay
import org.webrtc.VideoTrack
import kotlin.math.roundToInt

private val SurfaceDark = Color(0xFF1A1A28)

private enum class VideoCorner { TopStart, TopEnd, BottomStart, BottomEnd }

@Immutable
data class AudioDeviceData(
    val availableDevices: List<AudioDevice>,
    val selectedDevice: AudioDevice?
)

/**
 * Unified ongoing call screen covering Outgoing, Connecting, Connected and Reconnecting states.
 * A single composable for all active call states prevents layout remounts and eliminates
 * UI blink when transitioning between states (especially Outgoing→Connected for video calls).
 * The control bar is rendered once here and overlaid on whichever layout is active.
 */

@Composable
fun OngoingCallScreen(
    callState: CallUiState,
    mediaState: MediaState,
    duration: String,
    audioDeviceData: AudioDeviceData,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onSelectDevice: (AudioDevice) -> Unit,
    onEndCall: () -> Unit
) {
    val remoteName = callState.remoteUserName ?: callState.remoteUserId
    val remoteAvatar = callState.remoteUserAvatar

    val (availableDevices, selectedDevice) = audioDeviceData
    val isConnected = callState.phase == CallUiState.CallPhase.Connected ||
            callState.phase == CallUiState.CallPhase.Reconnecting

    val showVideoLayout = when (callState.phase) {
        CallUiState.CallPhase.Outgoing -> callState.isVideo && mediaState.localVideoTrack != null
        CallUiState.CallPhase.Connecting -> mediaState.shouldShowLocalPreview
        else -> mediaState.hasActiveVideo  // Connected or Reconnecting
    }

    var showAudioDeviceSelector by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    // Auto-hide controls in video mode after 4s once connected
    LaunchedEffect(showControls, isConnected, showVideoLayout) {
        if (showControls && isConnected && showVideoLayout) {
            delay(4000)
            showControls = false
        }
    }

    // Controls always visible in audio mode or pre-answer video
    val controlsVisible = !showVideoLayout || !isConnected || showControls

    Box(modifier = Modifier.fillMaxSize()) {
        if (showVideoLayout) {
            VideoOngoingLayout(
                callState = callState,
                remoteName = remoteName,
                duration = duration,
                mediaState = mediaState,
                showControls = controlsVisible,
                onTap = { showControls = !showControls }
            )
        } else {
            AudioOngoingLayout(
                callState = callState,
                remoteName = remoteName,
                remoteAvatar = remoteAvatar,
                isRemoteMuted = mediaState.isRemoteMuted,
                duration = duration
            )
        }

        // Single control bar — animated for video (tap to show/hide), always visible for audio
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CallControlBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
                    .padding(horizontal = 16.dp),
                isMuted = mediaState.isMuted,
                selectedAudioDevice = selectedDevice,
                isVideoEnabled = mediaState.isCameraEnabled,
                videoDisabled = !isConnected,
                onToggleMute = onToggleMute,
                onToggleSpeaker = { showAudioDeviceSelector = true },
                onToggleVideo = onToggleCamera,
                onHangup = onEndCall,
                onFlipCamera = onSwitchCamera
            )
        }

        if (showAudioDeviceSelector) {
            AudioDeviceSelector(
                availableDevices = availableDevices,
                selectedDevice = selectedDevice,
                onDeviceSelected = onSelectDevice,
                onDismiss = { showAudioDeviceSelector = false }
            )
        }
    }
}

// ── Audio layout ──────────────────────────────────────────────────────────────

@Composable
private fun AudioOngoingLayout(
    callState: CallUiState,
    remoteName: String,
    remoteAvatar: String?,
    isRemoteMuted: Boolean,
    duration: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallColors.BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.fillMaxHeight(0.1f))

            // Avatar with outer ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(164.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = CircleShape
                        )
                )
                UserAvatar(
                    avatarUrl = remoteAvatar,
                    name = remoteName,
                    size = 140.dp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = remoteName,
                    color = CallColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                if (isRemoteMuted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Remote microphone muted",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            CallStatusContent(
                callState = callState,
                duration = duration
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// ── Video layout ──────────────────────────────────────────────────────────────

@Composable
private fun VideoOngoingLayout(
    callState: CallUiState,
    remoteName: String,
    duration: String,
    mediaState: MediaState,
    showControls: Boolean,
    onTap: () -> Unit
) {
    val hasRemoteVideo = mediaState.remoteVideoTrack != null && mediaState.isRemoteVideoEnabled
    val hasLocalVideo = mediaState.isCameraEnabled && mediaState.localVideoTrack != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        // Main video content
        when {
            hasRemoteVideo -> {
                RemoteVideoView(
                    videoTrack = mediaState.remoteVideoTrack,
                    modifier = Modifier.fillMaxSize()
                )
                if (hasLocalVideo) {
                    DraggableLocalVideoPreview(
                        videoTrack = mediaState.localVideoTrack,
                        showControls = showControls,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            hasLocalVideo -> {
                LocalVideoPreview(
                    videoTrack = mediaState.localVideoTrack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CallColors.BackgroundDark)
                )
            }
        }

        // Top bar: name + status
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF17191C).copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(
                        top = 16.dp,
                        bottom = 40.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = remoteName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        if (mediaState.isRemoteMuted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = "Remote microphone muted",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    CallStatusContent(
                        callState = callState,
                        duration = duration
                    )
                }
            }
        }
    }
}

// ── Status content ────────────────────────────────────────────────────────────

@Composable
private fun CallStatusContent(
    callState: CallUiState,
    duration: String
) {
    when (callState.phase) {
        CallUiState.CallPhase.Outgoing ->
            DotsText(if (callState.isRemoteRinging) "ringing" else "calling")

        CallUiState.CallPhase.Connecting ->
            DotsText("connecting")

        CallUiState.CallPhase.Connected -> {
            Text(
                text = duration,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }

        CallUiState.CallPhase.Reconnecting -> {
            DotsText("reconnecting (${callState.reconnectAttempt}/${callState.maxReconnectAttempts})")
        }

        else -> {}
    }
}

@Composable
private fun DotsText(base: String) {
    val dots = ".".repeat(3)
    Text(
        text = "$base$dots",
        color = CallColors.TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )
}

// ── Control bar ───────────────────────────────────────────────────────────────

/**
 * 4(+1) button action bar.
 * Buttons: [flip-camera (shown only when video on)] | mute | speaker | video | hangup
 */
@Composable
@Preview
fun CallControlBar(
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    selectedAudioDevice: AudioDevice? = null,
    isVideoEnabled: Boolean = false,
    videoDisabled: Boolean = false,
    onToggleMute: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
    onToggleVideo: () -> Unit = {},
    onHangup: () -> Unit = {},
    onFlipCamera: () -> Unit = {}
) {
    val isSpeakerActive = selectedAudioDevice is AudioDevice.Speakerphone
    val audioRouteIcon = when (selectedAudioDevice) {
        is AudioDevice.BluetoothHeadset -> Icons.Default.BluetoothAudio
        is AudioDevice.WiredHeadset -> Icons.Default.Headset
        is AudioDevice.Earpiece -> Icons.AutoMirrored.Filled.VolumeUp
        else -> Icons.AutoMirrored.Filled.VolumeUp  // Speakerphone or null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CallColors.ActionBarBg, RoundedCornerShape(20.dp))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isVideoEnabled) {
            CallActionButton(
                icon = Icons.Default.Cameraswitch,
                backgroundColor = CallColors.ButtonSurface,
                iconTint = Color.White,
                contentDescription = "Flip Camera",
                onClick = onFlipCamera,
                size = 48.dp,
                iconSize = 28.dp
            )
        }

        CallActionButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            backgroundColor = CallColors.ButtonSurface,
            iconTint = Color.White,
            contentDescription = if (isMuted) "Unmute" else "Mute",
            onClick = onToggleMute,
            size = 48.dp,
            iconSize = 28.dp
        )

        CallActionButton(
            icon = audioRouteIcon,
            backgroundColor = if (isSpeakerActive) Color.White else CallColors.ButtonSurface,
            iconTint = if (isSpeakerActive) CallColors.BackgroundDark else Color.White,
            contentDescription = "Audio Route",
            onClick = onToggleSpeaker,
            size = 48.dp,
            iconSize = 28.dp
        )

        CallActionButton(
            icon = Icons.Default.Videocam,
            backgroundColor = if (isVideoEnabled) Color.White else CallColors.ButtonSurface,
            iconTint = if (isVideoEnabled) CallColors.BackgroundDark else Color.White,
            contentDescription = "Video",
            onClick = onToggleVideo,
            size = 48.dp,
            iconSize = 28.dp,
            enabled = !videoDisabled
        )

        CallActionButton(
            icon = Icons.Default.CallEnd,
            backgroundColor = CallColors.HangupRed,
            iconTint = Color.White,
            contentDescription = "End Call",
            onClick = onHangup,
            size = 48.dp,
            iconSize = 28.dp
        )
    }
}

// ── Video overlay composables ─────────────────────────────────────────────────

@Composable
private fun DraggableLocalVideoPreview(
    videoTrack: VideoTrack?,
    showControls: Boolean,
    modifier: Modifier = Modifier
) {
    var currentCorner by remember { mutableStateOf(VideoCorner.TopEnd) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val previewWidth = with(density) { 120.dp.toPx() }
        val previewHeight = with(density) { 160.dp.toPx() }
        val padding = with(density) { 16.dp.toPx() }
        val topPadding = with(density) { if (showControls) 100.dp.toPx() else 50.dp.toPx() }
        val bottomPadding = with(density) { if (showControls) 140.dp.toPx() else 50.dp.toPx() }

        val corners = mapOf(
            VideoCorner.TopStart to Offset(padding, topPadding),
            VideoCorner.TopEnd to Offset(constraints.maxWidth - previewWidth - padding, topPadding),
            VideoCorner.BottomStart to Offset(
                padding,
                constraints.maxHeight - previewHeight - bottomPadding
            ),
            VideoCorner.BottomEnd to Offset(
                constraints.maxWidth - previewWidth - padding,
                constraints.maxHeight - previewHeight - bottomPadding
            )
        )

        val basePosition = corners[currentCorner] ?: Offset.Zero

        val animatedPosition by animateOffsetAsState(
            targetValue = if (isDragging) basePosition + dragOffset else basePosition,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = if (isDragging) Spring.StiffnessHigh else Spring.StiffnessMediumLow
            ),
            label = "preview_position"
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        animatedPosition.x.roundToInt(),
                        animatedPosition.y.roundToInt()
                    )
                }
                .scale(scale)
                .size(width = 120.dp, height = 160.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    color = if (isDragging) CallColors.AccentBlue else Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(SurfaceDark)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            val currentPos = basePosition + dragOffset
                            currentCorner = corners.minByOrNull { (_, pos) ->
                                (currentPos - pos).getDistance()
                            }?.key ?: currentCorner
                            dragOffset = Offset.Zero
                            isDragging = false
                        },
                        onDragCancel = {
                            dragOffset = Offset.Zero
                            isDragging = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        }
                    )
                }
        ) {
            LocalVideoPreview(videoTrack = videoTrack, modifier = Modifier.fillMaxSize())
        }
    }
}
