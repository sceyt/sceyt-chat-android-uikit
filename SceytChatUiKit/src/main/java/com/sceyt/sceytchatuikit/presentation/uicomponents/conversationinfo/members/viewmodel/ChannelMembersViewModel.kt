package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.sceytchatuikit.data.channeleventobserver.*
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.toGroupChannel
import com.sceyt.sceytchatuikit.data.toMember
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
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
        setPagingLoadingStarted(LoadNext)
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
                    withContext(Dispatchers.Main) {
                        _membersLiveData.value = PaginationResponse.DBResponse(
                            mapToMemberItem(it.data, it.hasNext), 0, it.offset)
                        notifyPageStateWithResponse(SceytResponse.Success(null), it.offset > 0, it.data.isEmpty())
                    }
                }
            }
            is PaginationResponse.ServerResponse -> {
                if (it.data is SceytResponse.Success) {
                    withContext(Dispatchers.Main) {
                        _membersLiveData.value = PaginationResponse.ServerResponse(
                            SceytResponse.Success(mapToMemberItem(it.data.data, it.hasNext)),
                            it.cashData.map { MemberItem.Member(it) }, it.loadKey,
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
            val response = membersMiddleWare.changeChannelOwner(channelId, id)
            if (response is SceytResponse.Success) {
                val groupChannel = (response.data ?: return@launch).toGroupChannel()
                _changeOwnerLiveData.postValue((groupChannel.members.find { it.role.name == "owner" }
                        ?: return@launch).id)
            }
            notifyPageStateWithResponse(response)
        }
    }

    fun kickMember(channelId: Long, memberId: String, block: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = if (block) membersMiddleWare.blockAndDeleteMember(channelId, memberId)
            else membersMiddleWare.deleteMember(channelId, memberId)

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

    fun changeRole(channelId: Long, member: SceytMember) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersMiddleWare.changeChannelMemberRole(channelId, member)
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

    fun addMembersToChannel(channelId: Long, users: ArrayList<SceytMember>) {
        viewModelScope.launch(Dispatchers.IO) {
            val members = users.map { it.toMember() }
            val response = membersMiddleWare.addMembersToChannel(channelId, members)
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