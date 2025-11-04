package com.sceyt.chatuikit.presentation.components.poll_results.option_voters

import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.messages.Vote
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.repositories.PollRepository
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PollOptionVotersUIState(
    val voters: List<VoterItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class PollOptionVotersViewModel(
    private val messageId:Long,
    private val pollId: String,
    private val optionId: String,
    private val pollOptionVotersCount: Int,
    private val ownVote: Vote?,
    private val persistenceChannelsLogic: PersistenceChannelsLogic,
    private val pollRepository: PollRepository
) : BaseViewModel(){

    private val _uiState = MutableStateFlow(PollOptionVotersUIState())
    val uiState = _uiState.asStateFlow()

    private val _findOrCreateChatFlow = MutableSharedFlow<SceytChannel>()
    val findOrCreateChatFlow = _findOrCreateChatFlow.asSharedFlow()

    private var nextToken: String = ""
    private val votersList = mutableListOf<VoterItem>()

    init {
        observePollUpdates()
    }

      private fun observePollUpdates() {
        MessagesCache.messageUpdatedFlow
            .onEach { (_, messages) ->
                messages.find { it.id == messageId }?.let { updatedMessage ->
                    handlePollUpdate(updatedMessage)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handlePollUpdate(updatedMessage: SceytMessage) {
        val poll = updatedMessage.poll ?: return

        val allVotes = poll.votes.filter { it.optionId == optionId }
        val ownVoteForOption = poll.ownVotes.firstOrNull { it.optionId == optionId }

        val updatedVoteCount = if (ownVoteForOption != null) {
            allVotes.size + 1
        } else {
            allVotes.size
        }

        val headerIndex = votersList.indexOfFirst { it is VoterItem.HeaderItem }
        if (headerIndex >= 0) {
            votersList[headerIndex] = VoterItem.HeaderItem(updatedVoteCount)
        } else {
            votersList.add(0, VoterItem.HeaderItem(updatedVoteCount))
        }

        if (ownVote != null) {
            val hasOwn = votersList.any { it is VoterItem.Voter && it.vote.user?.id == ownVote.user?.id }
            if (!hasOwn) {
                val insertOwnIndex = kotlin.math.min(1, votersList.size)
                votersList.add(insertOwnIndex, VoterItem.Voter(ownVote))
            }
        }

        val currentVoterIds = allVotes.mapNotNull { it.user?.id }.toSet()

        val votersToRemove = votersList
            .filterIsInstance<VoterItem.Voter>()
            .filter { voterItem ->
                val userId = voterItem.vote.user?.id
                userId != null && userId !in currentVoterIds && userId != ownVote?.user?.id
            }

        votersList.removeAll(votersToRemove.toSet())

        val existingVoterIds = votersList
            .filterIsInstance<VoterItem.Voter>()
            .mapNotNull { it.vote.user?.id }
            .toSet()

        val newVoters = allVotes
            .filter { vote -> vote.user?.id !in existingVoterIds && vote.user?.id != ownVote?.user?.id }
            .map { VoterItem.Voter(it) }

        if (newVoters.isNotEmpty() || votersToRemove.isNotEmpty()) {
            votersList.removeAll { it is VoterItem.LoadingMore }

            val baseIndex = votersList.indexOfFirst { it is VoterItem.HeaderItem }.let { if (it == -1) 0 else it + 1 }
            val insertionIndex = baseIndex + if (ownVote != null) 1 else 0
            val safeIndex = insertionIndex.coerceIn(0, votersList.size)

            if (newVoters.isNotEmpty()) {
                votersList.addAll(safeIndex, newVoters)
            }

            if (_uiState.value.hasNext) {
                votersList.add(VoterItem.LoadingMore)
            }
        }

        _uiState.update { state ->
            state.copy(voters = votersList.toList())
        }
    }

    fun loadVotes(offset: Int) {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val response = pollRepository.getPollVotes(
                messageId = messageId,
                pollId = pollId,
                optionId = optionId,
                nextToken = if (offset > 0) nextToken else ""
            )

            when (response) {
                is SceytPagingResponse.Success -> {
                    nextToken = response.nextToken ?: ""

                    if (offset == 0) {
                        votersList.clear()
                        votersList.add(VoterItem.HeaderItem(pollOptionVotersCount))

                        if (ownVote != null) {
                            votersList.add(VoterItem.Voter(ownVote))
                        }
                    }

                    votersList.removeAll { it is VoterItem.LoadingMore }

                    votersList.addAll(response.data.map { VoterItem.Voter(it) })

                    if (response.hasNext) {
                        votersList.add(VoterItem.LoadingMore)
                    }

                    _uiState.update {
                        it.copy(
                            voters = votersList.toList(),
                            isLoading = false,
                            hasNext = response.hasNext,
                            error = null
                        )
                    }
                }

                is SceytPagingResponse.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasNext = false,
                            error = response.exception?.message
                        )
                    }
                }
            }
        }
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