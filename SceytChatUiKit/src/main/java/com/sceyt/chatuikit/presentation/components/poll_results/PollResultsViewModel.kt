package com.sceyt.chatuikit.presentation.components.poll_results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.PendingVoteData
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.messages.Vote
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.persistence.extensions.getRealCountsWithPendingVotes
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
        MessagesCache.messageUpdatedFlow
            .onEach { (_, messages) ->
                messages.find { it.id == message.id }?.let { updatedMessage ->
                    handlePollUpdate(updatedMessage)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handlePollUpdate(updatedMessage: SceytMessage) {
        updateUiFromMessage(updatedMessage)
    }

    private fun loadPollResults() {
        viewModelScope.launch(Dispatchers.Default) {
            updateUiFromMessage(message)
        }
    }

    private fun updateUiFromMessage(message: SceytMessage) {
        val items = if (message.poll == null) emptyList() else buildPollItems(message)
        _uiState.update {
            it.copy(items = items, isLoading = false, error = null)
        }
    }

    private fun buildPollItems(message: SceytMessage): List<PollResultItem> {
        val poll = message.poll ?: return emptyList()

        val headerItem = PollResultItem.HeaderItem(poll = poll)

        val votesByOptionId = poll.votes.groupBy { it.optionId }
        val ownVoteByOptionId = poll.ownVotes.associateBy { it.optionId }
        val pendingVotesByOptionId = poll.pendingVotes.orEmpty().groupBy { it.optionId }

        val realVoteCounts = poll.getRealCountsWithPendingVotes()

        val optionItems = poll.options.sortedBy { it.order }.map { option ->
            val otherVoters = votesByOptionId[option.id].orEmpty()
            val ownVote = ownVoteByOptionId[option.id]
            val pendingVotesForOption = pendingVotesByOptionId[option.id].orEmpty()

            val voters = buildVotersPreviewList(otherVoters, ownVote, pendingVotesForOption)

            val voteCount = realVoteCounts[option.id] ?: 0
            val hasMore = voteCount > VOTERS_PREVIEW_LIMIT

            PollResultItem.PollOptionItem(
                pollOption = option,
                voteCount = voteCount,
                voters = voters,
                hasMore = hasMore
            )
        }

        return listOf(headerItem) + optionItems
    }

    private fun buildVotersPreviewList(
        otherVoters: List<Vote>,
        ownVote: Vote?,
        pendingVotes: List<PendingVoteData>,
    ): List<Vote> {
        val pendingAdds = pendingVotes.filter { it.isAdd }
        val pendingRemoves = pendingVotes.filter { !it.isAdd }
        
        val allVoters = mutableListOf<Vote>()
        
        val ownPendingAdd = pendingAdds.firstOrNull()
        val ownPendingRemove = pendingRemoves.firstOrNull()
        
        when {
            ownPendingAdd != null -> {
                allVoters.add(Vote(
                    optionId = ownPendingAdd.optionId,
                    createdAt = ownPendingAdd.createdAt,
                    user = ownPendingAdd.user
                ))
            }
            ownVote != null && ownPendingRemove == null -> {
                allVoters.add(ownVote)
            }
        }
        
        val usersWithPendingRemoves = pendingRemoves.map { it.user.id }.toSet()
        otherVoters
            .filter { vote -> vote.user?.id?.let { !usersWithPendingRemoves.contains(it) } ?: false }
            .take(maxOf(0, VOTERS_PREVIEW_LIMIT - allVoters.size))
            .let { allVoters.addAll(it) }
        
        return allVoters.take(VOTERS_PREVIEW_LIMIT)
    }

    private companion object {
        const val VOTERS_PREVIEW_LIMIT = 5
    }

    fun findOrCreatePendingDirectChat(user: SceytUser) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceChannelsLogic.findOrCreatePendingChannelByMembers(
                CreateChannelData(
                    type = ChannelTypeEnum.Direct.value,
                    members = listOf(
                        SceytMember(
                            role = Role(RoleTypeEnum.Owner.value),
                            user = user
                        )
                    ),
                )
            ).onSuccessNotNull { data ->
                _findOrCreateChatFlow.emit(data)
            }
        }
    }
}