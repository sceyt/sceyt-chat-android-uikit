package com.sceyt.chatuikit.presentation.components.poll_results.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PollResultsUIState {
    data class Success(val items: List<PollResultItem>) : PollResultsUIState
    data object Loading : PollResultsUIState
    data object Empty : PollResultsUIState
}

class PollResultsViewModel(
        private val message: SceytMessage,
        private val persistenceChannelsLogic: PersistenceChannelsLogic
) : ViewModel() {

    private val _uiState = MutableStateFlow<PollResultsUIState>(PollResultsUIState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _findOrCreateChatFlow = MutableSharedFlow<SceytChannel>()
    val findOrCreateChatFlow = _findOrCreateChatFlow.asSharedFlow()

    init {
        loadPollResults()
    }

    private fun loadPollResults() {
        viewModelScope.launch(Dispatchers.Default) {
            val poll = message.poll
            if (poll == null) {
                _uiState.value = PollResultsUIState.Empty
                return@launch
            }

            val headerItem = PollResultItem.HeaderItem(poll = poll)

            val optionItems = poll.options.map { option ->
                val otherVoters = poll.votes.filter { it.optionId == option.id }
                
                val ownVote = poll.ownVotes.firstOrNull { it.optionId == option.id }
                
                val allVoters = if (ownVote != null) {
                    listOf(ownVote) + otherVoters
                } else {
                    otherVoters
                }
                
                val voteCount = allVoters.size
                val hasMore = allVoters.size > 5

                PollResultItem.PollOptionItem(
                    pollOption = option,
                    voteCount = voteCount,
                    voters = allVoters,
                    hasMore = hasMore
                )
            }

            val allItems = mutableListOf<PollResultItem>(headerItem)
            allItems.addAll(optionItems)

            _uiState.value = PollResultsUIState.Success(allItems)
        }
    }

    fun findOrCreatePendingDirectChat(user: SceytUser) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceChannelsLogic.findOrCreatePendingChannelByMembers(CreateChannelData(
                type = ChannelTypeEnum.Direct.value,
                members = listOf(SceytMember(role = Role(RoleTypeEnum.Owner.value), user = user)),
            )).onSuccessNotNull { data ->
                _findOrCreateChatFlow.emit(data)
            }
        }
    }
}