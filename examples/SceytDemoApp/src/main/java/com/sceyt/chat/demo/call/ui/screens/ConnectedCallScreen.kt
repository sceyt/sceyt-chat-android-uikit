package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.audiorouting.AudioDevice
import com.sceyt.chat.demo.call.manager.MediaState
import com.sceyt.chat.demo.call.ui.components.AudioDeviceSelector
import com.sceyt.chat.demo.call.ui.components.CallActionButton
import com.sceyt.chat.demo.call.ui.components.LocalVideoPreview
import com.sceyt.chat.demo.call.ui.components.RemoteVideoView
import com.sceyt.chat.demo.call.ui.components.UserAvatar
import kotlinx.coroutines.delay
import org.webrtc.VideoTrack
import kotlin.math.roundToInt

// Modern color palette
private val GradientStart = Color(0xFF1A1A2E)
private val GradientMiddle = Color(0xFF16213E)
private val GradientEnd = Color(0xFF0F0F1A)
private val AccentBlue = Color(0xFF4F8CFF)
private val AccentGreen = Color(0xFF34C759)
private val AccentRed = Color(0xFFFF453A)
private val SurfaceLight = Color(0xFF2A2A40)
private val SurfaceDark = Color(0xFF1A1A28)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB0B0C0)

/**
 * Corner positions for the draggable local video preview.
 */
private enum class VideoCorner {
    TopStart, TopEnd, BottomStart, BottomEnd
}

/**
 * Connected call screen with full controls.
 * Shows audio-only layout or video layout based on media state.
 */
@Composable
fun ConnectedCallScreen(
    remoteName: String,
    remoteAvatar: String?,
    duration: String,
    mediaState: MediaState,
    availableDevices: List<AudioDevice>,
    selectedDevice: AudioDevice?,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSelectDevice: (AudioDevice) -> Unit,
    onEndCall: () -> Unit
) {
    var showAudioDeviceSelector by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (mediaState.hasActiveVideo) {
            VideoCallLayout(
                remoteName = remoteName,
                remoteAvatar = remoteAvatar,
                duration = duration,
                mediaState = mediaState,
                selectedDevice = selectedDevice,
                onToggleMute = onToggleMute,
                onToggleCamera = onToggleCamera,
                onSwitchCamera = onSwitchCamera,
                onAudioRouteClick = { showAudioDeviceSelector = true },
                onEndCall = onEndCall
            )
        } else {
            AudioCallLayout(
                remoteName = remoteName,
                remoteAvatar = remoteAvatar,
                duration = duration,
                mediaState = mediaState,
                selectedDevice = selectedDevice,
                onToggleMute = onToggleMute,
                onToggleCamera = onToggleCamera,
                onAudioRouteClick = { showAudioDeviceSelector = true },
                onEndCall = onEndCall
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

@Composable
private fun AudioCallLayout(
    remoteName: String,
    remoteAvatar: String?,
    duration: String,
    mediaState: MediaState,
    selectedDevice: AudioDevice?,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onAudioRouteClick: () -> Unit,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMiddle, GradientEnd)
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
            Spacer(modifier = Modifier.height(60.dp))

            // Avatar with glow effect
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AccentBlue.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = size.width * 0.8f
                            )
                        )
                    }
            ) {
                UserAvatar(
                    avatarUrl = remoteAvatar,
                    name = remoteName,
                    size = 140.dp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Name
            Text(
                text = remoteName,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Duration with pill background
            Box(
                modifier = Modifier
                    .background(
                        color = AccentGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = duration,
                    color = AccentGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Remote muted indicator
            if (mediaState.isRemoteMuted) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = SurfaceLight.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Muted",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Modern control bar
            ModernCallControlBar(
                isMuted = mediaState.isMuted,
                selectedDevice = selectedDevice,
                isCameraEnabled = mediaState.isCameraEnabled,
                showCameraToggle = true,
                showSwitchCamera = false,
                onToggleMute = onToggleMute,
                onAudioRouteClick = onAudioRouteClick,
                onToggleCamera = onToggleCamera,
                onSwitchCamera = {},
                onEndCall = onEndCall
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun VideoCallLayout(
    remoteName: String,
    remoteAvatar: String?,
    duration: String,
    mediaState: MediaState,
    selectedDevice: AudioDevice?,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onAudioRouteClick: () -> Unit,
    onEndCall: () -> Unit
) {
    // Controls visibility state with auto-hide
    var showControls by remember { mutableStateOf(true) }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Determine video layout mode - check both track and enabled state
    val hasRemoteVideo = mediaState.remoteVideoTrack != null && mediaState.isRemoteVideoEnabled
    val hasLocalVideo = mediaState.isCameraEnabled && mediaState.localVideoTrack != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls }
                )
            }
    ) {
        // Main video content
        when {
            // Remote video available - show remote full screen
            hasRemoteVideo -> {
                RemoteVideoView(
                    videoTrack = mediaState.remoteVideoTrack,
                    modifier = Modifier.fillMaxSize()
                )

                // Show local video as small preview
                if (hasLocalVideo) {
                    DraggableLocalVideoPreview(
                        videoTrack = mediaState.localVideoTrack,
                        showControls = showControls,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Only local video - show local full screen
            hasLocalVideo -> {
                LocalVideoPreview(
                    videoTrack = mediaState.localVideoTrack,
                    modifier = Modifier.fillMaxSize()
                )

                // Show remote user info overlay
                RemoteUserOverlay(
                    remoteName = remoteName,
                    remoteAvatar = remoteAvatar,
                    isRemoteMuted = mediaState.isRemoteMuted,
                    showControls = showControls,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            // No video at all - show placeholder
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(GradientStart, GradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Waiting for video...",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Top bar with controls
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
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(vertical = 20.dp, horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Show remote user info when remote video is on
                    if (hasRemoteVideo) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                avatarUrl = remoteAvatar,
                                name = remoteName,
                                size = 32.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = remoteName,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = duration,
                                    color = AccentGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (mediaState.isRemoteMuted) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.MicOff,
                                    contentDescription = "Remote muted",
                                    tint = AccentRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = remoteName,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = duration,
                            color = AccentGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Bottom control bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                ModernCallControlBar(
                    isMuted = mediaState.isMuted,
                    selectedDevice = selectedDevice,
                    isCameraEnabled = mediaState.isCameraEnabled,
                    showCameraToggle = true,
                    showSwitchCamera = true,
                    onToggleMute = onToggleMute,
                    onAudioRouteClick = onAudioRouteClick,
                    onToggleCamera = onToggleCamera,
                    onSwitchCamera = onSwitchCamera,
                    onEndCall = onEndCall
                )
            }
        }
    }
}

/**
 * Remote user overlay shown when local video is full screen.
 */
@Composable
private fun RemoteUserOverlay(
    remoteName: String,
    remoteAvatar: String?,
    isRemoteMuted: Boolean,
    showControls: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showControls,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    color = SurfaceDark.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            UserAvatar(
                avatarUrl = remoteAvatar,
                name = remoteName,
                size = 36.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = remoteName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isRemoteMuted) "Camera off • Muted" else "Camera off",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Draggable local video preview that can be moved between corners.
 */
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
            VideoCorner.TopEnd to Offset(
                constraints.maxWidth - previewWidth - padding,
                topPadding
            ),
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
                .offset { IntOffset(animatedPosition.x.roundToInt(), animatedPosition.y.roundToInt()) }
                .scale(scale)
                .size(width = 120.dp, height = 160.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    color = if (isDragging) AccentBlue else Color.White.copy(alpha = 0.3f),
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
            LocalVideoPreview(
                videoTrack = videoTrack,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ModernCallControlBar(
    isMuted: Boolean,
    selectedDevice: AudioDevice?,
    isCameraEnabled: Boolean,
    showCameraToggle: Boolean,
    showSwitchCamera: Boolean,
    onToggleMute: () -> Unit,
    onAudioRouteClick: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SurfaceDark.copy(alpha = 0.8f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute button
        ModernControlButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            isActive = isMuted,
            activeColor = AccentRed,
            onClick = onToggleMute,
            contentDescription = if (isMuted) "Unmute" else "Mute"
        )

        // Audio route button
        val isBluetooth = selectedDevice is AudioDevice.BluetoothHeadset
        ModernControlButton(
            icon = selectedDevice.toIcon(),
            isActive = isBluetooth,
            activeColor = AccentBlue,
            onClick = onAudioRouteClick,
            contentDescription = "Audio Route"
        )

        // Camera toggle
        if (showCameraToggle) {
            ModernControlButton(
                icon = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                isActive = isCameraEnabled,
                activeColor = AccentBlue,
                onClick = onToggleCamera,
                contentDescription = if (isCameraEnabled) "Camera Off" else "Camera On"
            )
        }

        // Switch camera
        if (showSwitchCamera) {
            ModernControlButton(
                icon = Icons.Default.Cameraswitch,
                isActive = false,
                activeColor = AccentBlue,
                onClick = onSwitchCamera,
                contentDescription = "Switch Camera",
                enabled = isCameraEnabled
            )
        }

        // End call button - always prominent
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(8.dp, CircleShape)
                .background(AccentRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CallActionButton(
                icon = Icons.Default.CallEnd,
                backgroundColor = AccentRed,
                iconTint = Color.White,
                contentDescription = "End Call",
                onClick = onEndCall,
                size = 56.dp,
                iconSize = 26.dp
            )
        }
    }
}

@Composable
private fun ModernControlButton(
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> SurfaceLight.copy(alpha = 0.3f)
            isActive -> activeColor.copy(alpha = 0.2f)
            else -> SurfaceLight
        },
        label = "bg_color"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            !enabled -> TextSecondary.copy(alpha = 0.3f)
            isActive -> activeColor
            else -> TextPrimary
        },
        label = "icon_color"
    )

    CallActionButton(
        icon = icon,
        backgroundColor = backgroundColor,
        iconTint = iconColor,
        contentDescription = contentDescription,
        onClick = onClick,
        size = 52.dp,
        iconSize = 24.dp,
        enabled = enabled
    )
}

private fun AudioDevice?.toIcon(): ImageVector {
    return when (this) {
        is AudioDevice.BluetoothHeadset -> Icons.Default.BluetoothAudio
        is AudioDevice.WiredHeadset -> Icons.Default.Headset
        is AudioDevice.Earpiece -> Icons.Default.Phone
        is AudioDevice.Speakerphone -> Icons.AutoMirrored.Filled.VolumeUp
        null -> Icons.AutoMirrored.Filled.VolumeUp
    }
}
