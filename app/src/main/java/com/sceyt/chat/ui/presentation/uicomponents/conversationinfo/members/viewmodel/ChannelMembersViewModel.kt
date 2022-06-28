package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.ChannelsRepository
import com.sceyt.chat.ui.data.ChannelsRepositoryImpl
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventsObserverService
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelMembersEventData
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelMembersEventEnum
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelOwnerChangedEventData
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.data.toGroupChannel
import com.sceyt.chat.ui.data.toMember
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChannelMembersViewModel : BaseViewModel() {
    // Todo di
    private val repo: ChannelsRepository = ChannelsRepositoryImpl()
    var hasNext: Boolean = false
    var loadingMembers: Boolean = false

    private val _membersLiveData = MutableLiveData<List<MemberItem>>()
    val membersLiveData: LiveData<List<MemberItem>> = _membersLiveData

    private val _loadMoreMembersLiveData = MutableLiveData<List<MemberItem>>()
    val loadMoreMembersLiveData: LiveData<List<MemberItem>> = _loadMoreMembersLiveData

    private val _changeOwnerLiveData = MutableLiveData<String>()
    val changeOwnerLiveData: LiveData<String> = _changeOwnerLiveData

    private val _channelMemberEventLiveData = MutableLiveData<ChannelMembersEventData>()
    val channelMemberEventLiveData: LiveData<ChannelMembersEventData> = _channelMemberEventLiveData

    private val _channelOwnerChangedEventLiveData = MutableLiveData<ChannelOwnerChangedEventData>()
    val channelOwnerChangedEventLiveData: LiveData<ChannelOwnerChangedEventData> = _channelOwnerChangedEventLiveData

    init {
        viewModelScope.launch {
            ChannelEventsObserverService.onChannelMembersEventFlow.collect {
                _channelMemberEventLiveData.postValue(it)
            }
        }

        viewModelScope.launch {
            ChannelEventsObserverService.onChannelOwnerChangedEventFlow.collect {
                _channelOwnerChangedEventLiveData.postValue(it)
            }
        }
    }

    fun getChannelMembers(channelId: Long, loadingMore: Boolean) {
        loadingMembers = true
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.loadChannelMembers(channelId)
            if (response is SceytResponse.Success) {
                hasNext = response.data?.size == SceytUIKitConfig.CHANNELS_MEMBERS_LOAD_SIZE

                if (loadingMore)
                    _loadMoreMembersLiveData.postValue(mapToMemberItem(response.data, hasNext))
                else
                    _membersLiveData.postValue(mapToMemberItem(response.data, hasNext))
            }
            loadingMembers = false
            notifyPageStateWithResponse(response, loadingMore)
        }
    }

    private fun mapToMemberItem(list: List<SceytMember>?, hasNest: Boolean): MutableList<MemberItem> {
        val memberItems: MutableList<MemberItem> = (list
                ?: return arrayListOf()).map { MemberItem.Member(it) }.toMutableList()
        if (hasNest)
            memberItems.add(MemberItem.LoadingMore)
        return memberItems
    }

    fun changeOwner(channel: SceytChannel, id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.changeChannelOwner(channel.toGroupChannel(), id)
            notifyResponseAndPageState(_changeOwnerLiveData, response)
        }
    }

    fun kickMember(channel: SceytChannel, member: SceytMember, block: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = if (block) repo.blockAndDeleteMember(channel.toGroupChannel(), member.id)
            else repo.deleteMember(channel.toGroupChannel(), member.id)

            if (response is SceytResponse.Success) {
                _channelMemberEventLiveData.postValue(ChannelMembersEventData(
                    channel = channel.toGroupChannel(),
                    members = arrayListOf(member.toMember()),
                    eventType = if (block) ChannelMembersEventEnum.Blocked else ChannelMembersEventEnum.Kicked
                ))
            }

            notifyPageStateWithResponse(response)
        }
    }

    fun changeRole(channel: SceytChannel, member: SceytMember) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.changeChannelMemberRole(channel.toGroupChannel(), member.toMember())
            if (response is SceytResponse.Success) {
                _channelMemberEventLiveData.postValue(ChannelMembersEventData(
                    channel = channel.toGroupChannel(),
                    members = arrayListOf(member.toMember()),
                    eventType = ChannelMembersEventEnum.Role
                ))
            }

            notifyPageStateWithResponse(response)
        }
    }
}