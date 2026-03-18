package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sceyt.chat.call.R
import com.sceyt.chat.demo.call.manager.CallParticipantUiState
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

@OptIn(ExperimentalMaterial3Api::class)
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
                items(inCall, key = { it.userId }) { participant ->
                    InCallRow(participant = participant, modifier = Modifier.animateItem())
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
                items(notJoined, key = { it.userId }) { participant ->
                    NotJoinedRow(
                        participant = participant,
                        memberCallState = uiState.memberCallStates[participant.userId]
                            ?: MemberCallState.Idle,
                        onCall = { viewModel.onCallMember(participant.userId) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            item(key = "spacer_bottom") { Spacer(modifier = Modifier.height(8.dp)) }
        }
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

@Composable
private fun InCallRow(
    participant: CallParticipantUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            avatarUrl = participant.avatarUrl,
            name = participant.displayName,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = participant.displayName,
            modifier = Modifier.weight(1f),
            color = MembersTitleColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )

        AnimatedVisibility(
            visible = participant.isMuted,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.7f),
        ) {
            Icon(
                imageVector = Icons.Default.MicOff,
                contentDescription = "Muted",
                tint = MicOffColor,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = !participant.isVideoEnabled,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.7f),
        ) {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.VideocamOff,
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
    participant: CallParticipantUiState,
    memberCallState: MemberCallState,
    onCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showLottie = participant.isRinging || memberCallState == MemberCallState.Pending

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            avatarUrl = participant.avatarUrl,
            name = participant.displayName,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = participant.displayName,
            modifier = Modifier.weight(1f),
            color = MembersTitleColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )

        when {
            showLottie -> RingingLottie()
            memberCallState == MemberCallState.CallAgain -> CallChip(
                label = stringResource(R.string.call_again),
                onClick = onCall
            )

            else -> CallChip(label = stringResource(R.string.call), onClick = onCall)
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
