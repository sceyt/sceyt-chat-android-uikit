package com.sceyt.chatuikit.presentation.components.channel_info.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.managers.channel.event.ChannelEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelEventEnum
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventEnum
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.toMember
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ChannelInfoViewModel : BaseViewModel(), SceytKoinComponent {
    private val channelInteractor by inject<ChannelInteractor>()
    private val channelMemberInteractor by inject<ChannelMemberInteractor>()
    private val userInteractor by inject<UserInteractor>()

    private val _channelLiveData = MutableLiveData<SceytChannel>()
    val channelLiveData: LiveData<SceytChannel> = _channelLiveData

    private val _leaveChannelLiveData = MutableLiveData<Long>()
    val leaveChannelLiveData: LiveData<Long> = _leaveChannelLiveData

    private val _deleteChannelLiveData = MutableLiveData<Long>()
    val deleteChannelLiveData: LiveData<Long> = _deleteChannelLiveData

    private val _clearHistoryLiveData = MutableLiveData<Long>()
    val clearHistoryLiveData: LiveData<Long> = _clearHistoryLiveData

    private val _blockUnblockUserLiveData = MutableLiveData<List<User>>()
    val blockUnblockUserLiveData: LiveData<List<User>> = _blockUnblockUserLiveData

    private val _muteUnMuteLiveData = MutableLiveData<SceytChannel>()
    val muteUnMuteLiveData: LiveData<SceytChannel> = _muteUnMuteLiveData

    private val _autoDeleteLiveData = MutableLiveData<SceytChannel>()
    val autoDeleteLiveData: LiveData<SceytChannel> = _autoDeleteLiveData

    private val _pinUnpinLiveData = MutableLiveData<SceytChannel>()
    val pinUnpinLiveData: LiveData<SceytChannel> = _pinUnpinLiveData

    private val _joinLiveData = MutableLiveData<SceytChannel>()
    val joinLiveData: LiveData<SceytChannel> = _joinLiveData

    private val _channelAddMemberLiveData = MutableLiveData<ChannelMembersEventData>()
    val channelAddMemberLiveData: LiveData<ChannelMembersEventData> = _channelAddMemberLiveData

    private val _userPresenceUpdatedLiveData = MutableLiveData<Pair<SceytChannel, SceytPresenceChecker.PresenceUser>>()
    val userPresenceUpdateLiveData = _userPresenceUpdatedLiveData.asLiveData()

    private val _channelUpdatedLiveData = MutableLiveData<SceytChannel>()
    val channelUpdatedLiveData = _channelUpdatedLiveData.asLiveData()

    private val _onChannelLeftLiveData = MutableLiveData<ChannelEventData>()
    val onChannelLeftLiveData = _onChannelLeftLiveData.asLiveData()

    private val _onChannelDeletedLiveData = MutableLiveData<ChannelEventData>()
    val onChannelDeletedLiveData = _onChannelDeletedLiveData.asLiveData()

    fun observeUserPresenceUpdate(channel: SceytChannel) {
        SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged()
            .onEach { users ->
                users.find { user -> user.user.id == channel.getPeer()?.id }?.let { presenceUser ->
                    val members = channel.members?.toArrayList() ?: return@onEach
                    members.findIndexed { it.user.id == presenceUser.user.id }?.let { (index, member) ->
                        members[index] = member
                        _userPresenceUpdatedLiveData.postValue(channel.copy(members = members) to presenceUser)
                    }
                }
            }.launchIn(viewModelScope)
    }

    fun observeToChannelUpdate(channelId: Long) {
        ChannelsCache.channelUpdatedFlow
            .filter { it.channel.id == channelId }
            .onEach {
                _channelUpdatedLiveData.postValue(it.channel)
            }
            .launchIn(viewModelScope)

        ChannelsCache.pendingChannelCreatedFlow
            .filter { it.first == channelId }
            .onEach {
                _channelUpdatedLiveData.postValue(it.second)
            }
            .launchIn(viewModelScope)
    }

    fun onChannelEvent(channelId: Long) {
        ChannelEventManager.onChannelEventFlow
            .filter { it.channelId == channelId }
            .onEach {
                when (val event = it.eventType) {
                    is ChannelEventEnum.Left -> {
                        if (event.leftMembers.any { member -> member.id == SceytChatUIKit.chatUIFacade.myId })
                            _onChannelLeftLiveData.postValue(it)
                    }

                    is ChannelEventEnum.Deleted -> _onChannelDeletedLiveData.postValue(it)
                    is ChannelEventEnum.Joined -> _joinLiveData.postValue(it.channel
                            ?: return@onEach)

                    else -> return@onEach
                }
            }.launchIn(viewModelScope)
    }

    fun getChannelFromServer(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.getChannelFromServer(id)
            notifyResponseAndPageState(_channelLiveData, response, showError = false)
        }
    }

    fun clearHistory(channelId: Long, forEveryone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.clearHistory(channelId, forEveryone)
            notifyResponseAndPageState(_clearHistoryLiveData, response)
        }
    }

    fun leaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.leaveChannel(channelId)
            notifyResponseAndPageState(_leaveChannelLiveData, response)
        }
    }

    fun blockAndLeaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.blockAndLeaveChannel(channelId)
            notifyResponseAndPageState(_leaveChannelLiveData, response)
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.deleteChannel(channelId)
            notifyResponseAndPageState(_deleteChannelLiveData, response)
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = userInteractor.blockUnBlockUser(userId, true)
            notifyResponseAndPageState(_blockUnblockUserLiveData, response)
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = userInteractor.blockUnBlockUser(userId, false)
            notifyResponseAndPageState(_blockUnblockUserLiveData, response)
        }
    }

    fun muteChannel(channelId: Long, muteUntil: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.muteChannel(channelId, muteUntil)
            notifyResponseAndPageState(_muteUnMuteLiveData, response)
        }
    }

    fun unMuteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.unMuteChannel(channelId)
            notifyResponseAndPageState(_muteUnMuteLiveData, response)
        }
    }

    fun enableAutoDelete(channelId: Long, period: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.enableAutoDelete(channelId, period)
            notifyResponseAndPageState(_autoDeleteLiveData, response)
        }
    }

    fun disableAutoDelete(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.disableAutoDelete(channelId)
            notifyResponseAndPageState(_autoDeleteLiveData, response)
        }
    }

    fun joinChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.join(channelId)
            notifyResponseAndPageState(_joinLiveData, response)
        }
    }


    fun pinChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.pinChannel(channelId)
            notifyResponseAndPageState(_pinUnpinLiveData, response)
        }
    }

    fun unpinChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.unpinChannel(channelId)
            notifyResponseAndPageState(_pinUnpinLiveData, response)
        }
    }

    fun addMembersToChannel(channelId: Long, users: List<SceytMember>) {
        viewModelScope.launch(Dispatchers.IO) {
            val members = users.map { it.toMember() }
            val response = channelMemberInteractor.addMembersToChannel(channelId, members)
            if (response is SceytResponse.Success) {
                val groupChannel = (response.data ?: return@launch)

                _channelAddMemberLiveData.postValue(ChannelMembersEventData(
                    channel = groupChannel,
                    members = groupChannel.members ?: return@launch,
                    eventType = ChannelMembersEventEnum.Added
                ))
            }

            notifyPageStateWithResponse(response)
        }
    }
}