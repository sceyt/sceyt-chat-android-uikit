package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.toMember
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.persistence.extensions.asLiveData
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelUpdateData
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ConversationInfoViewModel : BaseViewModel(), SceytKoinComponent {
    private val channelsMiddleWare by inject<PersistenceChanelMiddleWare>()
    private val membersMiddleWare by inject<PersistenceMembersMiddleWare>()

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

    private val _joinLiveData = MutableLiveData<SceytChannel>()
    val joinLiveData: LiveData<SceytChannel> = _joinLiveData

    private val _channelAddMemberLiveData = MutableLiveData<ChannelMembersEventData>()
    val channelAddMemberLiveData: LiveData<ChannelMembersEventData> = _channelAddMemberLiveData

    private val _findOrCreateChatLiveData = MutableLiveData<SceytChannel>()
    val findOrCreateChatLiveData = _findOrCreateChatLiveData.asLiveData()

    private val _userPresenceUpdatedLiveData = MutableLiveData<SceytPresenceChecker.PresenceUser>()
    val userPresenceUpdateLiveData = _userPresenceUpdatedLiveData.asLiveData()

    private val _channelUpdatedLiveData = MutableLiveData<ChannelUpdateData>()
    val channelUpdatedLiveData = _channelUpdatedLiveData.asLiveData()

    fun observeUserPresenceUpdate(channel: SceytChannel) {
        SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged()
            .onEach {
                it.find { user -> user.user.id == channel.getFirstMember()?.id }?.let { presenceUser ->
                    _userPresenceUpdatedLiveData.postValue(presenceUser)
                }
            }.launchIn(viewModelScope)
    }

    fun observeToChannelUpdate(channelId: Long) {
        ChannelsCache.channelUpdatedFlow
            .filter { it.channel.id == channelId }
            .onEach {
                _channelUpdatedLiveData.postValue(it)
            }
            .launchIn(viewModelScope)
    }

    fun getChannelFromServer(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.getChannelFromServer(id)
            notifyResponseAndPageState(_channelLiveData, response, showError = false)
        }
    }

    fun clearHistory(channelId: Long, forEveryone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.clearHistory(channelId, forEveryone)
            notifyResponseAndPageState(_clearHistoryLiveData, response)
        }
    }

    fun leaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.leaveChannel(channelId)
            notifyResponseAndPageState(_leaveChannelLiveData, response)
        }
    }

    fun blockAndLeaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.blockAndLeaveChannel(channelId)
            notifyResponseAndPageState(_leaveChannelLiveData, response)
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.deleteChannel(channelId)
            notifyResponseAndPageState(_deleteChannelLiveData, response)
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersMiddleWare.blockUnBlockUser(userId, true)
            notifyResponseAndPageState(_blockUnblockUserLiveData, response)
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersMiddleWare.blockUnBlockUser(userId, false)
            notifyResponseAndPageState(_blockUnblockUserLiveData, response)
        }
    }

    fun muteChannel(channelId: Long, muteUntil: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.muteChannel(channelId, muteUntil)
            notifyResponseAndPageState(_muteUnMuteLiveData, response)
        }
    }

    fun unMuteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.unMuteChannel(channelId)
            notifyResponseAndPageState(_muteUnMuteLiveData, response)
        }
    }

    fun joinChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.join(channelId)
            notifyResponseAndPageState(_joinLiveData, response)
        }
    }

    fun addMembersToChannel(channelId: Long, users: ArrayList<SceytMember>) {
        viewModelScope.launch(Dispatchers.IO) {
            val members = users.map { it.toMember() }
            val response = membersMiddleWare.addMembersToChannel(channelId, members)
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

    fun findOrCreateChat(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.findOrCreateDirectChannel(user)
            if (response is SceytResponse.Success)
                _findOrCreateChatLiveData.postValue(response.data ?: return@launch)

            notifyPageStateWithResponse(response)
        }
    }
}