package com.sceyt.chatuikit.presentation.components.poll_results

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
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PollResultsUIState(
    val items: List<PollResultItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class PollResultsViewModel(
        private val message: SceytMessage,
        private val persistenceChannelsLogic: PersistenceChannelsLogic
) : ViewModel() {

    private val _uiState = MutableStateFlow(PollResultsUIState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _findOrCreateChatFlow = MutableSharedFlow<SceytChannel>()
    val findOrCreateChatFlow = _findOrCreateChatFlow.asSharedFlow()

    init {
        loadPollResults()
        observePollUpdates()
    }

    private fun observePollUpdates() {
        viewModelScope.launch {
            MessagesCache.messageUpdatedFlow
                .collect { (_, messages) ->
                    messages.find { it.id == message.id }?.let { updatedMessage ->
                        handlePollUpdate(updatedMessage)
                    }
                }
        }
    }

    private fun handlePollUpdate(updatedMessage: SceytMessage) {
        loadPollResultsFromMessage(updatedMessage)
    }

    private fun loadPollResults() {
        viewModelScope.launch(Dispatchers.Default) {
            loadPollResultsFromMessage(message)
        }
    }

    private fun loadPollResultsFromMessage(message: SceytMessage) {
        val poll = message.poll
        if (poll == null) {
            _uiState.update {
                it.copy(items = emptyList(), isLoading = false, error = null)
            }
            return
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
            
            val voteCount = poll.votesPerOption[option.id] ?: 0
            val hasMore = voteCount > 5

            PollResultItem.PollOptionItem(
                pollOption = option,
                voteCount = voteCount,
                voters = allVoters,
                hasMore = hasMore
            )
        }

        val allItems = mutableListOf<PollResultItem>(headerItem)
        allItems.addAll(optionItems)

        _uiState.update {
            it.copy(items = allItems, isLoading = false, error = null)
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