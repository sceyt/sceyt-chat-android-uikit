package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.ChannelsRepository
import com.sceyt.chat.ui.data.ChannelsRepositoryImpl
import com.sceyt.chat.ui.data.channeleventobserverservice.*
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.data.toGroupChannel
import com.sceyt.chat.ui.data.toMember
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelMembersViewModel : BaseViewModel() {
    // Todo di
    private val repo: ChannelsRepository = ChannelsRepositoryImpl()

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

    private val _channelEventEventLiveData = MutableLiveData<ChannelEventData>()
    val channelEventEventLiveData: LiveData<ChannelEventData> = _channelEventEventLiveData

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

        viewModelScope.launch {
            ChannelEventsObserverService.onChannelEventFlow.collect {
                _channelEventEventLiveData.postValue(it)
            }
        }
    }

    fun getChannelMembers(channelId: Long, loadingMore: Boolean) {
        loadingItems = true
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.loadChannelMembers(channelId)
            if (response is SceytResponse.Success) {
                hasNext = response.data?.size == SceytUIKitConfig.CHANNELS_MEMBERS_LOAD_SIZE

                if (loadingMore)
                    _loadMoreMembersLiveData.postValue(mapToMemberItem(response.data, hasNext))
                else
                    _membersLiveData.postValue(mapToMemberItem(response.data, hasNext))
            }
            loadingItems = false
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
            if (response is SceytResponse.Success) {
                val groupChannel = (response.data ?: return@launch).toGroupChannel()
                _changeOwnerLiveData.postValue((groupChannel.members.find { it.role.name == "owner" }
                        ?: return@launch).id)
            }
            notifyPageStateWithResponse(response)
        }
    }

    fun kickMember(channel: SceytChannel, memberId: String, block: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = if (block) repo.blockAndDeleteMember(channel.toGroupChannel(), memberId)
            else repo.deleteMember(channel.toGroupChannel(), memberId)

            if (response is SceytResponse.Success) {
                val groupChannel = (response.data ?: return@launch).toGroupChannel()
                _channelMemberEventLiveData.postValue(ChannelMembersEventData(
                    channel = groupChannel,
                    members = groupChannel.members,
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
                val groupChannel = (response.data ?: return@launch).toGroupChannel()
                _channelMemberEventLiveData.postValue(ChannelMembersEventData(
                    channel = groupChannel,
                    members = groupChannel.members,
                    eventType = ChannelMembersEventEnum.Role
                ))
            }

            notifyPageStateWithResponse(response)
        }
    }

    fun addMembersToChannel(channel: SceytChannel, users: ArrayList<SceytMember>) {
        viewModelScope.launch(Dispatchers.IO) {
            val members = users.map { it.toMember() }
            val response = repo.addMembersToChannel(channel.toGroupChannel(), members)
            if (response is SceytResponse.Success) {
                val groupChannel = (response.data ?: return@launch).toGroupChannel()
                if (groupChannel.members.isNullOrEmpty()) return@launch

                _channelMemberEventLiveData.postValue(ChannelMembersEventData(
                    channel = groupChannel,
                    members = groupChannel.members,
                    eventType = ChannelMembersEventEnum.Added
                ))
            }

            notifyPageStateWithResponse(response)
        }
    }
}