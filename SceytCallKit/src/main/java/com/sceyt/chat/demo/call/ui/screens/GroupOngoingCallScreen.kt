package com.sceyt.chat.demo.call.ui.screens

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sceyt.audiorouting.AudioDevice
import com.sceyt.chat.demo.call.manager.CallParticipantUiState
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.GROUP_CALL_PAGE_SIZE
import com.sceyt.chat.demo.call.manager.buildPageRows
import com.sceyt.chat.demo.call.manager.displayTitle
import com.sceyt.chat.demo.call.manager.paginateParticipants
import com.sceyt.chat.demo.call.manager.resolveStatusText
import com.sceyt.chat.demo.call.ui.components.AudioDeviceSelector
import com.sceyt.chat.demo.call.ui.components.UserAvatar
import com.sceyt.chat.demo.call.ui.components.VideoRenderer
import com.sceyt.chat.demo.call.ui.theme.CallColors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import org.webrtc.RendererCommon

private val SurfaceDark = Color(0xFF232324)
private val GroupTileShape = RoundedCornerShape(8.dp)
private val GroupGridBottomPadding = 4.dp

@Composable
internal fun GroupOngoingCallScreen(
    callState: CallUiState,
    duration: String,
    audioDeviceData: AudioDeviceData,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onSelectDevice: (AudioDevice) -> Unit,
    onEndCall: () -> Unit,
    onAddParticipant: () -> Unit,
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val title = callState.call?.displayTitle(callState.remoteParticipants)
        ?: callState.remoteParticipant?.displayName.orEmpty()
    val visibleParticipants = remember(callState.localParticipant, callState.remoteParticipants) {
        listOfNotNull(callState.localParticipant) +
            callState.remoteParticipants.filter { it.isVisibleInGroupGrid }
    }
    val pages = remember(visibleParticipants) {
        paginateParticipants(visibleParticipants, GROUP_CALL_PAGE_SIZE)
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val (availableDevices, selectedDevice) = audioDeviceData
    var showAudioDeviceSelector by remember { mutableStateOf(false) }
    var showMembersSheet by remember { mutableStateOf(false) }
    var showChrome by remember { mutableStateOf(true) }
    val isConnected = callState.phase == CallUiState.CallPhase.Connected ||
            callState.phase == CallUiState.CallPhase.Reconnecting
    val chromeVisible = !isConnected || showChrome

    LaunchedEffect(showChrome, isConnected, pagerState.currentPage) {
        if (showChrome && isConnected) {
            delay(4000)
            showChrome = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { showChrome = !showChrome })
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = GroupGridBottomPadding)
        ) { pageIndex ->
            GroupParticipantsPage(
                participants = pages.getOrNull(pageIndex).orEmpty().toImmutableList(),
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            GroupTopBar(
                title = title,
                status = callState.resolveStatusText(duration),
                onBack = { backDispatcher?.onBackPressed() },
                onAddParticipant = { showMembersSheet = true },
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (pages.size > 1) {
                    PageDots(
                        pageCount = pages.size,
                        currentPage = pagerState.currentPage,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                CallControlBar(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    isMuted = callState.localParticipant?.isMuted == true,
                    selectedAudioDevice = selectedDevice,
                    isVideoEnabled = callState.localParticipant?.isVideoEnabled == true,
                    videoDisabled = false,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = { showAudioDeviceSelector = true },
                    onToggleVideo = onToggleCamera,
                    onHangup = onEndCall,
                    onFlipCamera = onSwitchCamera
                )
            }
        }

        if (showAudioDeviceSelector) {
            AudioDeviceSelector(
                availableDevices = availableDevices,
                selectedDevice = selectedDevice,
                onDeviceSelected = onSelectDevice,
                onDismiss = { showAudioDeviceSelector = false }
            )
        }

        if (showMembersSheet) {
            CallMembersBottomSheet(
                onDismiss = { showMembersSheet = false },
            )
        }
    }
}

@Composable
private fun GroupTopBar(
    title: String,
    status: String,
    onBack: () -> Unit,
    onAddParticipant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xE617191C),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 33.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = status,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onAddParticipant) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = "Add participant",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun GroupParticipantsPage(
    participants: ImmutableList<CallParticipantUiState>,
    modifier: Modifier = Modifier,
) {
    val rows = buildPageRows(participants)
    val boundsTransform = remember {
        BoundsTransform { _, _ ->
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        }
    }

    LookaheadScope {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { participant ->
                        key(participant.userId) {
                            val visibleState = remember {
                                MutableTransitionState(false).apply { targetState = true }
                            }
                            AnimatedVisibility(
                                visibleState = visibleState,
                                enter = scaleIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    initialScale = 0.8f
                                ),
                                exit = ExitTransition.None,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .animateBounds(
                                        lookaheadScope = this@LookaheadScope,
                                        boundsTransform = boundsTransform
                                    )
                            ) {
                                ParticipantTile(
                                    participant = participant,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantTile(
    participant: CallParticipantUiState,
    modifier: Modifier = Modifier,
) {
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (participant.isActiveSpeaker) Color(0xFF5DC475) else Color.Transparent,
        animationSpec = tween(durationMillis = 400),
        label = "active_speaker_border"
    )
    val hasVideo = participant.videoTrack != null && participant.isVideoEnabled

    Box(
        modifier = modifier
            .clip(GroupTileShape)
            .background(SurfaceDark)
            .border(width = 1.5.dp, color = borderColor, shape = GroupTileShape)
    ) {
        if (hasVideo) {
            VideoRenderer(
                videoTrack = participant.videoTrack,
                modifier = Modifier.fillMaxSize(),
                mirror = participant.isSelf,
                scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL
            )
        } else {
            ParticipantTileBackground(
                avatarUrl = participant.avatarUrl,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UserAvatar(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    avatarUrl = participant.avatarUrl,
                    name = participant.displayName
                )
                Spacer(modifier = Modifier.height(10.dp))
                TileLabel(participant = participant)
            }
        }
    }
}

@Composable
private fun ParticipantTileBackground(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.then(
            if (!avatarUrl.isNullOrBlank()) {
                Modifier.blur(90.dp)
            } else Modifier
        )
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            modifier = modifier.background(
                Color(0xFF19191B).copy(0.6f)
            )
        )
    }
}

@Composable
private fun TileLabel(
    participant: CallParticipantUiState,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = participant.displayName,
                color = CallColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (participant.isMuted) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = "Muted",
                    tint = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PageDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.28f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (index == currentPage) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.28f)
                        }
                    )
            )
        }
    }
}
