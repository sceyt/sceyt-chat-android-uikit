package com.sceyt.chat.demo.call.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sceyt.chat.call.R
import com.sceyt.chat.demo.call.ui.CallMemberUiState
import com.sceyt.chat.demo.call.ui.CallMembersViewModel
import com.sceyt.chat.demo.call.ui.MemberCallState
import com.sceyt.chat.demo.call.ui.components.UserAvatar
import org.koin.androidx.compose.koinViewModel

private val SheetBg = Color(0xFF232324)
private val MembersSheetHandleColor = Color(0xFF3B3B3D)
private val MembersTitleColor = Color(0xFFE1E3E6)
private val MembersSectionHeaderColor = Color(0xFF969A9F)
private val MembersCallButtonBg = Color(0xFF303032)
private val MembersCallButtonText = Color(0xFF6B72FF)
private val MicOffColor = Color(0xFF969A9F)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun CallMembersBottomSheet(
    onDismiss: () -> Unit,
    viewModel: CallMembersViewModel = koinViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()
    val inCall = uiState.inCall
    val notJoined = uiState.notJoined
    val totalCount = uiState.totalCount
    var participantForControl by remember { mutableStateOf<CallMemberUiState?>(null) }
    val context = LocalContext.current

    LaunchedEffect(viewModel, context) {
        viewModel.errors.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MembersSheetHandleColor),
            )
        },
    ) {
        Text(
            text = stringResource(R.string.call_participants, totalCount),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MembersTitleColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            if (inCall.isNotEmpty()) {
                item(key = "header_in_call") {
                    MembersSectionHeader(
                        text = stringResource(R.string.in_call),
                        modifier = Modifier.animateItem(),
                    )
                }
                items(
                    items = inCall,
                    key = { it.participant.userId }
                ) { member ->
                    InCallRow(
                        member = member,
                        isOwner = uiState.isOwner,
                        onLongClick = {
                            if (!member.participant.isSelf) {
                                participantForControl = member
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (notJoined.isNotEmpty()) {
                if (inCall.isNotEmpty()) {
                    item(key = "spacer_sections") {
                        Spacer(
                            modifier = Modifier
                                .height(4.dp)
                                .animateItem()
                        )
                    }
                }
                item(key = "header_not_joined") {
                    MembersSectionHeader(
                        text = stringResource(R.string.haven_t_joined),
                        modifier = Modifier.animateItem(),
                    )
                }
                items(
                    items = notJoined,
                    key = { it.participant.userId }
                ) { member ->
                    NotJoinedRow(
                        member = member,
                        onCall = { viewModel.onCallMember(member.participant.userId) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            item(key = "spacer_bottom") { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    participantForControl?.let { member ->
        ParticipantControlBottomSheet(
            member = member,
            onDismiss = { participantForControl = null },
            viewModel = viewModel,
        )
    }
}

@Composable
private fun MembersSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MembersSectionHeaderColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InCallRow(
    member: CallMemberUiState,
    isOwner: Boolean,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(
                if (isOwner && !member.participant.isSelf) {
                    Modifier.combinedClickable(onLongClick = onLongClick, onClick = {})
                } else Modifier
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            avatarUrl = member.participant.avatarUrl,
            name = member.participant.displayName,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = member.participant.displayName,
            modifier = Modifier.weight(1f),
            color = MembersTitleColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )

        val enterAnim = remember {
            fadeIn(animationSpec = tween(120)) + scaleIn(
                initialScale = 0.7f,
                animationSpec = tween(120)
            )
        }
        val exitAnim = remember {
            fadeOut(animationSpec = tween(120)) + scaleOut(
                targetScale = 0.7f,
                animationSpec = tween(120)
            )
        }
        AnimatedVisibility(
            visible = member.participant.isMuted,
            enter = enterAnim,
            exit = exitAnim
        ) {
            Icon(
                imageVector = Icons.Default.MicOff,
                contentDescription = "Muted",
                tint = MicOffColor,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = !member.participant.isVideoEnabled,
            enter = enterAnim,
            exit = exitAnim
        ) {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_call_video_off),
                    contentDescription = "Camera off",
                    tint = MicOffColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun NotJoinedRow(
    member: CallMemberUiState,
    onCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            avatarUrl = member.participant.avatarUrl,
            name = member.participant.displayName,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = member.participant.displayName,
            modifier = Modifier.weight(1f),
            color = MembersTitleColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )

        when (member.state) {
            MemberCallState.Joined -> Unit
            MemberCallState.Ringing -> RingingLottie()
            MemberCallState.Idle -> CallChip(
                label = stringResource(R.string.call),
                onClick = onCall
            )
        }
    }
}

@Composable
private fun RingingLottie() {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("call_ringing.json"))
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = Modifier.size(width = 40.dp, height = 20.dp),
    )
}

@Composable
private fun CallChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MembersCallButtonBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MembersCallButtonText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
