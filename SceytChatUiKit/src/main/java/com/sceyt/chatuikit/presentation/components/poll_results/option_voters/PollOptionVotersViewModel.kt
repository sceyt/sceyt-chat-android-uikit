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
import com.sceyt.chatuikit.persistence.extensions.getOwnVoteForOption
import com.sceyt.chatuikit.persistence.extensions.getVoteCountForOption
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
    private val message: SceytMessage,
    private val optionId: String,
    private val persistenceChannelsLogic: PersistenceChannelsLogic,
    private val pollRepository: PollRepository
) : BaseViewModel(){

    private val _uiState = MutableStateFlow(PollOptionVotersUIState())
    val uiState = _uiState.asStateFlow()

    private val _findOrCreateChatFlow = MutableSharedFlow<SceytChannel>()
    val findOrCreateChatFlow = _findOrCreateChatFlow.asSharedFlow()

    private val messageId: Long = message.id
    private val pollId: String = message.poll?.id ?: ""
    private var nextToken: String = ""
    private var hasMoreData: Boolean = false
    private var isLoadingMore: Boolean = false
    private val votersList = mutableListOf<VoterItem>()

    init {
        observePollUpdates()
        loadInitialData()
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

        val pendingVotesForOption = poll.pendingVotes.orEmpty().filter { it.optionId == optionId }
        val pendingAdds = pendingVotesForOption.filter { it.isAdd }
        val pendingRemoves = pendingVotesForOption.filter { !it.isAdd }

        val updatedVoteCount = poll.getVoteCountForOption(optionId)

        val headerIndex = votersList.indexOfFirst { it is VoterItem.HeaderItem }
        if (headerIndex >= 0) {
            votersList[headerIndex] = VoterItem.HeaderItem(updatedVoteCount)
        } else {
            votersList.add(0, VoterItem.HeaderItem(updatedVoteCount))
        }

        val ownVoteForOption = poll.getOwnVoteForOption(optionId)
        val ownPendingAdd = pendingAdds.firstOrNull()
        val ownPendingRemove = pendingRemoves.firstOrNull()

        votersList.removeAll { it is VoterItem.Voter && it.vote.user?.id == ownVoteForOption?.user?.id }

        when {
            ownPendingAdd != null -> {
                val insertOwnIndex = kotlin.math.min(1, votersList.size)
                votersList.add(insertOwnIndex, VoterItem.Voter(Vote(
                    optionId = ownPendingAdd.optionId,
                    createdAt = ownPendingAdd.createdAt,
                    user = ownPendingAdd.user
                )))
            }
            ownVoteForOption != null && ownPendingRemove == null -> {
                val insertOwnIndex = kotlin.math.min(1, votersList.size)
                votersList.add(insertOwnIndex, VoterItem.Voter(ownVoteForOption))
            }
        }

        val allVotes = poll.votes.filter { it.optionId == optionId }
        
        val usersWithPendingRemoves = pendingRemoves.map { it.user.id }.toSet()
        
        val currentVoterIds = allVotes
            .mapNotNull { it.user?.id }
            .filter { !usersWithPendingRemoves.contains(it) }
            .toSet()

        val pendingAddUserIds = pendingAdds.map { it.user.id }.toSet()
        val allCurrentVoterIds = currentVoterIds + pendingAddUserIds

        val votersToRemove = votersList
            .filterIsInstance<VoterItem.Voter>()
            .filter { voterItem ->
                val userId = voterItem.vote.user?.id
                userId != null && userId !in allCurrentVoterIds && userId != ownVoteForOption?.user?.id
            }

        votersList.removeAll(votersToRemove.toSet())

        val existingVoterIds = votersList
            .filterIsInstance<VoterItem.Voter>()
            .mapNotNull { it.vote.user?.id }
            .toSet()

        val newVoters = allVotes
            .filter { vote -> 
                val userId = vote.user?.id
                userId != null && 
                userId !in existingVoterIds && 
                userId != ownVoteForOption?.user?.id &&
                !usersWithPendingRemoves.contains(userId)
            }
            .map { VoterItem.Voter(it) }

        val newPendingVoters = pendingAdds
            .filter { pending ->
                val userId = pending.user.id
                userId !in existingVoterIds && userId != ownVoteForOption?.user?.id
            }
            .map { pending ->
                VoterItem.Voter(Vote(
                    optionId = pending.optionId,
                    createdAt = pending.createdAt,
                    user = pending.user
                ))
            }

        if (newVoters.isNotEmpty() || newPendingVoters.isNotEmpty() || votersToRemove.isNotEmpty()) {
            votersList.removeAll { it is VoterItem.LoadingMore }

            val baseIndex = votersList.indexOfFirst { it is VoterItem.HeaderItem }.let { if (it == -1) 0 else it + 1 }
            val hasOwnVote = votersList.any { it is VoterItem.Voter && it.vote.user?.id == ownVoteForOption?.user?.id }
            val insertionIndex = baseIndex + if (hasOwnVote) 1 else 0
            val safeIndex = insertionIndex.coerceIn(0, votersList.size)

            if (newVoters.isNotEmpty()) {
                votersList.addAll(safeIndex, newVoters)
            }
            
            if (newPendingVoters.isNotEmpty()) {
                votersList.addAll(safeIndex, newPendingVoters)
            }

            if (_uiState.value.hasNext) {
                votersList.add(VoterItem.LoadingMore)
            }
        }

        _uiState.update { state ->
            state.copy(voters = votersList.toList())
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }

            nextToken = ""
            hasMoreData = false
            isLoadingMore = false
            votersList.clear()

            val response = pollRepository.getPollVotes(
                messageId = messageId,
                pollId = pollId,
                optionId = optionId,
                nextToken = ""
            )

            when (response) {
                is SceytPagingResponse.Success -> {
                    nextToken = response.nextToken ?: ""
                    hasMoreData = response.hasNext

                    val poll = message.poll
                    val pollOptionVotersCount = poll?.getVoteCountForOption(optionId) ?: 0

                    votersList.add(VoterItem.HeaderItem(pollOptionVotersCount))

                    val ownVote = poll?.getOwnVoteForOption(optionId)
                    if (ownVote != null) {
                        votersList.add(VoterItem.Voter(ownVote))
                    }

                    votersList.addAll(response.data.map { VoterItem.Voter(it) })

                    if (response.hasNext) {
                        votersList.add(VoterItem.LoadingMore)
                    }

                    updateUIState()
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

    fun loadMore() {
        if (!hasMoreData || isLoadingMore) return

        viewModelScope.launch(Dispatchers.IO) {
            isLoadingMore = true

            val response = pollRepository.getPollVotes(
                messageId = messageId,
                pollId = pollId,
                optionId = optionId,
                nextToken = nextToken
            )

            when (response) {
                is SceytPagingResponse.Success -> {
                    nextToken = response.nextToken ?: ""
                    hasMoreData = response.hasNext

                    votersList.removeAll { it is VoterItem.LoadingMore }

                    votersList.addAll(response.data.map { VoterItem.Voter(it) })

                    if (response.hasNext) {
                        votersList.add(VoterItem.LoadingMore)
                    }

                    updateUIState()
                }

                is SceytPagingResponse.Error -> {
                    _uiState.update {
                        it.copy(
                            hasNext = false,
                            error = response.exception?.message
                        )
                    }
                }
            }

            isLoadingMore = false
        }
    }

    private fun updateUIState() {
        _uiState.update {
            it.copy(
                voters = votersList.toList(),
                isLoading = false,
                hasNext = hasMoreData,
                error = null
            )
        }
    }

    fun canLoadMore(): Boolean = hasMoreData && !isLoadingMore

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