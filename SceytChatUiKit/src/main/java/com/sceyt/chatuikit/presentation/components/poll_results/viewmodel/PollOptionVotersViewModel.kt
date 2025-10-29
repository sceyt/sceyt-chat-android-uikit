package com.sceyt.chatuikit.presentation.components.poll_results.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.repositories.PollRepository
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PollOptionVotersViewModel(
        private val pollId: String,
        private val optionId: String,
        private val pollOptionVotersCount:Int,
        private val persistenceChannelsLogic: PersistenceChannelsLogic,
        private val pollRepository: PollRepository
) : BaseViewModel(), SceytKoinComponent {

    private val _loadVotersLiveData = MutableLiveData<List<VoterItem>>()
    val loadVotersLiveData: LiveData<List<VoterItem>> = _loadVotersLiveData

    private val _findOrCreateChatFlow = MutableSharedFlow<SceytChannel>()
    val findOrCreateChatFlow = _findOrCreateChatFlow.asSharedFlow()

    private var nextToken: String = ""
    private var isLoading = false
    private val votersList = mutableListOf<VoterItem>()

    fun loadVotes(offset: Int) {
        if (isLoading) return
        isLoading = true

        val isLoadingMore = offset > 0
        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            val response = pollRepository.getPollVotes(
                pollId = pollId,
                optionId = optionId,
                nextToken = if (offset > 0) nextToken else ""
            )

            when (response) {
                is SceytPagingResponse.Success -> {
                    nextToken = response.nextToken ?: ""
                    hasNext = response.hasNext
                    
                    if (offset == 0) {
                        votersList.clear()
                        votersList.add(VoterItem.HeaderItem(pollOptionVotersCount))
                    }
                    
                    votersList.removeAll { it is VoterItem.LoadingMore }
                    
                    votersList.addAll(response.data.map { VoterItem.Voter(it) })
                    
                    if (hasNext) {
                        votersList.add(VoterItem.LoadingMore)
                    }
                    
                    withContext(Dispatchers.Main) {
                        _loadVotersLiveData.value = votersList.toList()
                        isLoading = false
                    }
                }

                is SceytPagingResponse.Error -> {
                    hasNext = false
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
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