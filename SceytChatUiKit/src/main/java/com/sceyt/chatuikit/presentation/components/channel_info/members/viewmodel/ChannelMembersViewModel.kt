package com.sceyt.chatuikit.presentation.components.channel_info.members.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelOwnerChangedEventData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.MembersMetaData
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.messages.SystemMsgBodyEnum
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMembersLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.mappers.toUser
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.MemberItem
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelMembersViewModel(
    private val channelId: Long,
    private val channelsLogic: PersistenceChannelsLogic,
    private val membersLogic: PersistenceMembersLogic,
    private val messageLogic: PersistenceMessagesLogic
) : BaseViewModel() {

    private val _membersLiveData = MutableLiveData<PaginationResponse<MemberItem>>()
    val membersLiveData: LiveData<PaginationResponse<MemberItem>> = _membersLiveData

    private val _changeOwnerLiveData = MutableLiveData<String>()
    val changeOwnerLiveData: LiveData<String> = _changeOwnerLiveData

    private val _channelMemberEventLiveData = MutableLiveData<ChannelMembersEventData>()
    val channelMemberEventLiveData: LiveData<ChannelMembersEventData> = _channelMemberEventLiveData

    private val _channelOwnerChangedEventLiveData = MutableLiveData<ChannelOwnerChangedEventData>()
    val channelOwnerChangedEventLiveData = _channelOwnerChangedEventLiveData.asLiveData()

    private val _channelEventLiveData = MutableLiveData<ChannelActionEvent>()
    val channelEventLiveData = _channelEventLiveData.asLiveData()

    private val _channelAddMemberLiveData = MutableLiveData<SceytChannel>()
    val channelAddMemberLiveData = _channelAddMemberLiveData.asLiveData()

    private val _channelRemoveMemberLiveData = MutableLiveData<SceytChannel>()
    val channelRemoveMemberLiveData = _channelRemoveMemberLiveData.asLiveData()

    private val _channelRoleLiveData = MutableLiveData<SceytChannel>()
    val channelRoleLiveData = _channelRoleLiveData.asLiveData()

    private val _findOrCreateChatLiveData = MutableLiveData<SceytChannel>()
    val findOrCreateChatLiveData = _findOrCreateChatLiveData.asLiveData()

    private var nextToken: String = ""

    init {
        viewModelScope.launch {
            ChannelEventManager.onChannelMembersEventFlow
                .filter { it.channel.id == channelId }
                .collect {
                    _channelMemberEventLiveData.postValue(it)
                }
        }

        viewModelScope.launch {
            ChannelEventManager.onChannelOwnerChangedEventFlow
                .filter { it.channel.id == channelId }
                .collect {
                    _channelOwnerChangedEventLiveData.postValue(it)
                }
        }

        viewModelScope.launch {
            ChannelEventManager.onChannelEventFlow
                .filter { event -> event.channelId == channelId }
                .collect {
                    _channelEventLiveData.postValue(it)
                }
        }
    }

    fun getChannelMembers(channelId: Long, offset: Int, role: String?) {
        setPagingLoadingStarted(LoadNext)
        notifyPageLoadingState(offset > 0)

        viewModelScope.launch(Dispatchers.IO) {
            membersLogic.loadChannelMembers(
                channelId = channelId,
                offsetDb = offset,
                nextToken = nextToken,
                role = role
            ).collect {
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
                            mapToMemberItem(list = it.data, hasNest = it.hasNext), null, it.offset
                        )
                        notifyPageStateWithResponse(
                            response = SceytResponse.Success(null),
                            wasLoadingMore = it.offset > 0,
                            isEmpty = it.data.isEmpty()
                        )
                    }
                }
            }

            is PaginationResponse.ServerResponse -> {
                if (it.data is SceytResponse.Success) {
                    withContext(Dispatchers.Main) {
                        nextToken = it.nextToken
                        _membersLiveData.value = PaginationResponse.ServerResponse(
                            data = SceytResponse.Success(
                                mapToMemberItem(
                                    list = it.data.data,
                                    hasNest = it.hasNext
                                )
                            ),
                            cacheData = it.cacheData.map { MemberItem.Member(it) },
                            loadKey = it.loadKey,
                            offset = it.offset,
                            hasDiff = it.hasDiff,
                            hasNext = it.hasNext,
                            hasPrev = it.hasPrev,
                            loadType = it.loadType,
                            ignoredDb = it.ignoredDb,
                            nextToken = it.nextToken
                        )
                    }
                }
                notifyPageStateWithResponse(it.data, it.offset > 0)
            }

            is PaginationResponse.Nothing -> return
        }
        pagingResponseReceived(it)
    }

    private fun mapToMemberItem(
        list: List<SceytMember>?,
        hasNest: Boolean
    ): MutableList<MemberItem> {
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

            response.onSuccessNotNull { channel ->
                _channelRemoveMemberLiveData.postValue(channel)
            }

            notifyPageStateWithResponse(response)
        }
    }

    fun changeRole(channelId: Long, vararg member: SceytMember) {
        if (member.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersLogic.changeChannelMemberRole(channelId, *member)
            response.onSuccessNotNull { channel ->
                _channelRoleLiveData.postValue(channel)
            }

            notifyPageStateWithResponse(response)
        }
    }

    fun addMembersToChannel(channelId: Long, members: List<SceytMember>) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersLogic.addMembersToChannel(
                channelId = channelId,
                members = members
            ).onSuccessNotNull { channel ->
                _channelAddMemberLiveData.postValue(channel)
            }

            notifyPageStateWithResponse(response)
        }
    }

    fun findOrCreatePendingDirectChat(user: SceytUser) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsLogic.findOrCreatePendingChannelByMembers(
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
                _findOrCreateChatLiveData.postValue(data)
            }

            notifyPageStateWithResponse(response)
        }
    }

    fun sendRemovedMemberMessage(
        channelId: Long,
        removedMembers: List<SceytMember>
    ) = viewModelScope.launch {
        messageLogic.sendMessage(
            channelId = channelId,
            message = Message(
                Message.MessageBuilder()
                    .setType(SceytMessageType.System.value)
                    .setMetadata(Gson().toJson(MembersMetaData(removedMembers.map { it.id })))
                    .setMentionedUsers(removedMembers.map { it.user.toUser() }.toTypedArray())
                    .withDisplayCount(0)
                    .setSilent(true)
                    .setBody(SystemMsgBodyEnum.MemberRemoved.value)
            )
        )
    }

    fun sendAddedMemberMessage(
        channelId: Long,
        addedMembers: List<SceytMember>
    ) = viewModelScope.launch {
        messageLogic.sendMessage(
            channelId = channelId,
            message = Message(
                Message.MessageBuilder()
                    .setType(SceytMessageType.System.value)
                    .setMetadata(Gson().toJson(MembersMetaData(addedMembers.map { it.id })))
                    .setMentionedUsers(addedMembers.map { it.user.toUser() }.toTypedArray())
                    .withDisplayCount(0)
                    .setSilent(true)
                    .setBody(SystemMsgBodyEnum.MemberAdded.value)
            )
        )
    }
}