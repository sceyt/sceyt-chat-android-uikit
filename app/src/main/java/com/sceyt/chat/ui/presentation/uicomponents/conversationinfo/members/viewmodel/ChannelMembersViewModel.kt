package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.channeleventobserver.*
import com.sceyt.chat.ui.data.models.PaginationResponse
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.data.toGroupChannel
import com.sceyt.chat.ui.data.toMember
import com.sceyt.chat.ui.persistence.PersistenceMembersMiddleWare
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelMembersViewModel(private val membersMiddleWare: PersistenceMembersMiddleWare) : BaseViewModel() {

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

    init {
        viewModelScope.launch {
            ChannelEventsObserver.onChannelMembersEventFlow.collect {
                _channelMemberEventLiveData.postValue(it)
            }
        }

        viewModelScope.launch {
            ChannelEventsObserver.onChannelOwnerChangedEventFlow.collect {
                _channelOwnerChangedEventLiveData.postValue(it)
            }
        }

        viewModelScope.launch {
            ChannelEventsObserver.onChannelEventFlow.collect {
                _channelEventEventLiveData.postValue(it)
            }
        }
    }

    fun getChannelMembers(channelId: Long, offset: Int) {
        loadingItems.set(true)
        notifyPageLoadingState(offset > 0)

        viewModelScope.launch(Dispatchers.IO) {
            membersMiddleWare.loadChannelMembers(channelId, offset).collect {
                initResponse(it)
            }
        }
    }

    private suspend fun initResponse(it: PaginationResponse<SceytMember>) {
        when (it) {
            is PaginationResponse.DBResponse -> {
                if (it.data.isNotEmpty()) {
                    hasNext = it.hasNext
                    withContext(Dispatchers.Main) {
                        _membersLiveData.value = PaginationResponse.DBResponse(
                            mapToMemberItem(it.data, it.hasNext), it.offset)
                        notifyPageStateWithResponse(SceytResponse.Success(null), it.offset > 0, it.data.isEmpty())
                    }
                }
            }
            is PaginationResponse.ServerResponse -> {
                if (it.data is SceytResponse.Success) {
                    hasNext = it.hasNext
                    withContext(Dispatchers.Main) {
                        _membersLiveData.value = PaginationResponse.ServerResponse(
                            SceytResponse.Success(mapToMemberItem(it.data.data, hasNext)),
                            it.dbData.map { MemberItem.Member(it) },
                            it.offset, it.hasNext)
                    }
                }
                notifyPageStateWithResponse(it.data, it.offset > 0)
            }
            is PaginationResponse.Nothing -> return
        }
        loadingItems.set(false)
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
            val response = membersMiddleWare.changeChannelOwner(channel, id)
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
            val response = if (block) membersMiddleWare.blockAndDeleteMember(channel, memberId)
            else membersMiddleWare.deleteMember(channel, memberId)

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
            val response = membersMiddleWare.changeChannelMemberRole(channel, member)
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
            val response = membersMiddleWare.addMembersToChannel(channel, members)
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