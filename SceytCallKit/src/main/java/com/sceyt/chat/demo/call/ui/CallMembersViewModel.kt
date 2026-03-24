package com.sceyt.chat.demo.call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.manager.CallParticipantUiState
import com.sceyt.chat.models.signal.ParticipantState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MemberCallState { Idle, Ringing, Joined }

data class CallMemberUiState(
    val participant: CallParticipantUiState,
    val state: MemberCallState = MemberCallState.Idle,
)

data class CallMembersUiState(
    val inCall: List<CallMemberUiState> = emptyList(),
    val notJoined: List<CallMemberUiState> = emptyList(),
) {
    val totalCount: Int get() = inCall.size + notJoined.size
}

class CallMembersViewModel(
    private val callManager: CallManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallMembersUiState())
    val uiState: StateFlow<CallMembersUiState> = _uiState

    private val ringingTimeoutJobs = mutableMapOf<String, Job>()

    init {
        callManager.callUiState.onEach { callState ->
            _uiState.update { state ->
                val oldRingings by lazy {
                    state.notJoined
                        .filter { it.state == MemberCallState.Ringing }
                        .map { it.participant.userId }
                        .toSet()
                }
                val allRemote = callState.remoteParticipants.map { participant ->
                    val memberState = when (participant.participantState) {
                        ParticipantState.Joined -> MemberCallState.Joined
                        ParticipantState.Ringing -> MemberCallState.Ringing
                        ParticipantState.Idle if oldRingings.contains(participant.userId) -> MemberCallState.Ringing
                        else -> MemberCallState.Idle
                    }
                    if (memberState == MemberCallState.Joined || memberState == MemberCallState.Ringing) {
                        ringingTimeoutJobs.remove(participant.userId)?.cancel()
                    }
                    CallMemberUiState(participant = participant, state = memberState)
                }

                val localParticipantUiState = callState.localParticipant?.let {
                    CallMemberUiState(it, MemberCallState.Joined)
                }
                val (inCall, notJoined) = allRemote.partition { state ->
                    state.state == MemberCallState.Joined
                }

                CallMembersUiState(
                    inCall = listOfNotNull(localParticipantUiState) + inCall.sortedBy { it.participant.displayName },
                    notJoined = notJoined.sortedBy { it.participant.displayName },
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onCallMember(userId: String) {
        val member = _uiState.value.notJoined.find { it.participant.userId == userId }
        if (member == null || member.state != MemberCallState.Idle) return
        ringingTimeoutJobs.remove(userId)?.cancel()
        _uiState.update { current ->
            current.copy(
                notJoined = current.notJoined.map { m ->
                    if (m.participant.userId == userId)
                        m.copy(state = MemberCallState.Ringing)
                    else m
                }
            )
        }
        callManager.reinvite(userId)
        scheduleRingingTimeout(userId)
    }

    private fun scheduleRingingTimeout(userId: String) {
        ringingTimeoutJobs.remove(userId)?.cancel()

        ringingTimeoutJobs[userId] = viewModelScope.launch {
            delay(10_000)
            _uiState.update { current ->
                var changed = false
                val updatedNotJoined = current.notJoined.map { item ->
                    if (item.participant.userId == userId && item.state == MemberCallState.Ringing) {
                        changed = true
                        item.copy(state = MemberCallState.Idle)
                    } else item
                }

                if (changed)
                    current.copy(notJoined = updatedNotJoined) else current
            }
            ringingTimeoutJobs.remove(userId)
        }
    }
}