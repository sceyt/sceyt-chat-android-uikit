package com.sceyt.chat.demo.call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.manager.CallParticipantUiState
import com.sceyt.chat.models.signal.ParticipantState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MemberCallState { Idle, Pending, CallAgain }

data class CallMembersUiState(
    val inCall: List<CallParticipantUiState> = emptyList(),
    val notJoined: List<CallParticipantUiState> = emptyList(),
    val memberCallStates: Map<String, MemberCallState> = emptyMap(),
) {
    val totalCount: Int get() = inCall.size + notJoined.size
}

class CallMembersViewModel(
    private val callManager: CallManager
) : ViewModel() {

    private val _memberCallStates = MutableStateFlow<Map<String, MemberCallState>>(emptyMap())

    val uiState: StateFlow<CallMembersUiState> = combine(
        callManager.callUiState,
        _memberCallStates,
    ) { callState, memberCallStates ->
        val ringingIds = callState.remoteParticipants
            .filter { it.participantState == ParticipantState.Ringing }
            .map { it.userId }
            .toSet()
        if (ringingIds.isNotEmpty()) {
            _memberCallStates.update { current -> current.filterKeys { it !in ringingIds } }
        }
        CallMembersUiState(
            inCall = listOfNotNull(callState.localParticipant) +
                    callState.remoteParticipants.filter { it.isConnected },
            notJoined = callState.remoteParticipants.filter { !it.isConnected },
            memberCallStates = memberCallStates,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CallMembersUiState())

    fun onCallMember(userId: String) {
        _memberCallStates.update { it + (userId to MemberCallState.Pending) }
        callManager.reinvite(userId)
        viewModelScope.launch {
            delay(5_000)
            _memberCallStates.update { current ->
                if (current[userId] == MemberCallState.Pending) {
                    current + (userId to MemberCallState.CallAgain)
                } else current
            }
        }
    }
}
