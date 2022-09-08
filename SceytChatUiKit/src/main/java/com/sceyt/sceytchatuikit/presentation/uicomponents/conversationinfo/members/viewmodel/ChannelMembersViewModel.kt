package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.toGroupChannel
import com.sceyt.sceytchatuikit.data.toMember
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelMembersViewModel(private val membersMiddleWare: com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare) : BaseViewModel() {

    private val _membersLiveData = MutableLiveData<PaginationResponse<MemberItem>>()
    val membersLiveData: LiveData<PaginationResponse<MemberItem>> = _membersLiveData

    private val _changeOwnerLiveData = MutableLiveData<String>()
    val changeOwnerLiveData: LiveData<String> = _changeOwnerLiveData

    private val _channelMemberEventLiveData = MutableLiveData<com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData>()
    val channelMemberEventLiveData: LiveData<com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData> = _channelMemberEventLiveData

    private val _channelOwnerChangedEventLiveData = MutableLiveData<com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData>()
    val channelOwnerChangedEventLiveData: LiveData<com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData> = _channelOwnerChangedEventLiveData

    private val _channelEventEventLiveData = MutableLiveData<com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData>()
    val channelEventEventLiveData: LiveData<com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData> = _channelEventEventLiveData

    init {
        viewModelScope.launch {
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver.onChannelMembersEventFlow.collect {
                _channelMemberEventLiveData.postValue(it)
            }
        }

        viewModelScope.launch {
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver.onChannelOwnerChangedEventFlow.collect {
                _channelOwnerChangedEventLiveData.postValue(it)
            }
        }

        viewModelScope.launch {
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver.onChannelEventFlow.collect {
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
                _channelMemberEventLiveData.postValue(com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData(
                    channel = groupChannel,
                    members = groupChannel.members,
                    eventType = if (block) com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Blocked else com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Kicked
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
                _channelMemberEventLiveData.postValue(com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData(
                    channel = groupChannel,
                    members = groupChannel.members,
                    eventType = com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Role
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

                _channelMemberEventLiveData.postValue(com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData(
                    channel = groupChannel,
                    members = groupChannel.members,
                    eventType = com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Added
                ))
            }

            notifyPageStateWithResponse(response)
        }
    }
}