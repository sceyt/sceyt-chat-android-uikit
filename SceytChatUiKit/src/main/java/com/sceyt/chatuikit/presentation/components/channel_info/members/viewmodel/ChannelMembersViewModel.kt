package com.sceyt.chatuikit.presentation.components.channel_info.members.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.managers.channel.event.ChannelEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventEnum
import com.sceyt.chatuikit.data.managers.channel.event.ChannelOwnerChangedEventData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.toMember
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMembersLogic
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.MemberItem
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelMembersViewModel(private val channelsLogic: PersistenceChannelsLogic,
                              private val membersLogic: PersistenceMembersLogic) : BaseViewModel() {

    private val _membersLiveData = MutableLiveData<PaginationResponse<MemberItem>>()
    val membersLiveData: LiveData<PaginationResponse<MemberItem>> = _membersLiveData

    private val _changeOwnerLiveData = MutableLiveData<String>()
    val changeOwnerLiveData: LiveData<String> = _changeOwnerLiveData

    private val _channelMemberEventLiveData = MutableLiveData<ChannelMembersEventData>()
    val channelMemberEventLiveData: LiveData<ChannelMembersEventData> = _channelMemberEventLiveData

    private val _channelOwnerChangedEventLiveData = MutableLiveData<ChannelOwnerChangedEventData>()
    val channelOwnerChangedEventLiveData: LiveData<ChannelOwnerChangedEventData> = _channelOwnerChangedEventLiveData

    private val _channelEventEventLiveData = MutableLiveData<ChannelEventData>()
    val channelEventEventLiveData: LiveData<ChannelEventData> = _channelEventEventLiveData

    private val _channelAddMemberLiveData = MutableLiveData<List<SceytMember>>()
    val channelAddMemberLiveData = _channelAddMemberLiveData.asLiveData()

    private val _channelRemoveMemberLiveData = MutableLiveData<List<SceytMember>>()
    val channelRemoveMemberLiveData = _channelRemoveMemberLiveData.asLiveData()

    private val _findOrCreateChatLiveData = MutableLiveData<SceytChannel>()
    val findOrCreateChatLiveData = _findOrCreateChatLiveData.asLiveData()

    init {
        viewModelScope.launch {
            ChannelEventManager.onChannelMembersEventFlow.collect {
                _channelMemberEventLiveData.postValue(it)
            }
        }

        viewModelScope.launch {
            ChannelEventManager.onChannelOwnerChangedEventFlow.collect {
                _channelOwnerChangedEventLiveData.postValue(it)
            }
        }

        viewModelScope.launch {
            ChannelEventManager.onChannelEventFlow.collect {
                _channelEventEventLiveData.postValue(it)
            }
        }
    }

    fun getChannelMembers(channelId: Long, offset: Int, role: String?) {
        setPagingLoadingStarted(LoadNext)
        notifyPageLoadingState(offset > 0)

        viewModelScope.launch(Dispatchers.IO) {
            membersLogic.loadChannelMembers(channelId, offset, role).collect {
                initResponse(it)
            }
        }
    }

    private suspend fun initResponse(it: PaginationResponse<SceytMember>) {
        when (it) {
            is PaginationResponse.DBResponse -> {
                if (it.data.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _membersLiveData.value = PaginationResponse.DBResponse(
                            mapToMemberItem(it.data, it.hasNext), null, it.offset)
                        notifyPageStateWithResponse(SceytResponse.Success(null), it.offset > 0, it.data.isEmpty())
                    }
                }
            }

            is PaginationResponse.ServerResponse -> {
                if (it.data is SceytResponse.Success) {
                    withContext(Dispatchers.Main) {
                        _membersLiveData.value = PaginationResponse.ServerResponse(
                            SceytResponse.Success(mapToMemberItem(it.data.data, it.hasNext)),
                            it.cacheData.map { MemberItem.Member(it) }, it.loadKey,
                            it.offset, it.hasDiff, it.hasNext, it.hasPrev, it.loadType, it.ignoredDb)
                    }
                }
                notifyPageStateWithResponse(it.data, it.offset > 0)
            }

            is PaginationResponse.Nothing -> return
        }
        pagingResponseReceived(it)
    }

    private fun mapToMemberItem(list: List<SceytMember>?, hasNest: Boolean): MutableList<MemberItem> {
        val memberItems: MutableList<MemberItem> = (list
                ?: return arrayListOf()).map { MemberItem.Member(it) }.toMutableList()
        if (hasNest)
            memberItems.add(MemberItem.LoadingMore)
        return memberItems
    }

    fun changeOwner(channelId: Long, id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersLogic.changeChannelOwner(channelId, id)
            if (response is SceytResponse.Success) {
                val groupChannel = (response.data ?: return@launch)
                _changeOwnerLiveData.postValue((groupChannel.members?.find {
                    it.role.name == SceytChatUIKit.config.memberRolesConfig.owner
                } ?: return@launch).id)
            }
            notifyPageStateWithResponse(response)
        }
    }

    fun kickMember(channelId: Long, memberId: String, block: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = if (block) membersLogic.blockAndDeleteMember(channelId, memberId)
            else membersLogic.deleteMember(channelId, memberId)

            if (response is SceytResponse.Success) {
                val channel = response.data ?: return@launch
                val members = channel.members ?: return@launch
                _channelMemberEventLiveData.postValue(ChannelMembersEventData(
                    channel = channel,
                    members = members,
                    eventType = if (block) ChannelMembersEventEnum.Blocked else ChannelMembersEventEnum.Kicked
                ))

                _channelRemoveMemberLiveData.postValue(members)
            }

            notifyPageStateWithResponse(response)
        }
    }

    fun changeRole(channelId: Long, vararg member: SceytMember) {
        if (member.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersLogic.changeChannelMemberRole(channelId, *member)
            if (response is SceytResponse.Success) {
                val channel = response.data ?: return@launch
                val members = channel.members ?: return@launch
                _channelMemberEventLiveData.postValue(ChannelMembersEventData(
                    channel = channel,
                    members = members,
                    eventType = ChannelMembersEventEnum.Role
                ))
            }

            notifyPageStateWithResponse(response)
        }
    }

    fun addMembersToChannel(channelId: Long, users: ArrayList<SceytMember>) {
        viewModelScope.launch(Dispatchers.IO) {
            val membersToAdd = users.map { it.toMember() }
            val response = membersLogic.addMembersToChannel(channelId, membersToAdd)
            if (response is SceytResponse.Success) {
                val channel = response.data ?: return@launch
                val members = channel.members ?: return@launch

                _channelMemberEventLiveData.postValue(ChannelMembersEventData(
                    channel = channel,
                    members = members,
                    eventType = ChannelMembersEventEnum.Added
                ))
                _channelAddMemberLiveData.postValue(members)
            }

            notifyPageStateWithResponse(response)
        }
    }

    fun findOrCreateChat(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsLogic.findOrCreateDirectChannel(user)
            if (response is SceytResponse.Success)
                _findOrCreateChatLiveData.postValue(response.data ?: return@launch)

            notifyPageStateWithResponse(response)
        }
    }
}