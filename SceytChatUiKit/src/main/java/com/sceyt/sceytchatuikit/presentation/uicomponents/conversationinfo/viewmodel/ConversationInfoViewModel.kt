package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.SceytKoinComponent
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ConversationInfoViewModel : BaseViewModel(), SceytKoinComponent {
    // Todo di
    private val channelsMiddleWare by inject<PersistenceChanelMiddleWare>()
    private val membersMiddleWare by inject<PersistenceMembersMiddleWare>()

    private val _channelLiveData = MutableLiveData<SceytChannel>()
    val channelLiveData: LiveData<SceytChannel> = _channelLiveData

    private val _editChannelLiveData = MutableLiveData<SceytChannel>()
    val editChannelLiveData: LiveData<SceytChannel> = _editChannelLiveData

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


    fun getChannelFromServer(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.getChannelFromServer(id)
            notifyResponseAndPageState(_channelLiveData, response)
        }
    }

    fun saveChanges(channel: SceytGroupChannel, newSubject: String, avatarUrl: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.editChannel(channel, newSubject, avatarUrl)
            notifyResponseAndPageState(_editChannelLiveData, response)
        }
    }

    fun clearHistory(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.clearHistory(channel)
            notifyResponseAndPageState(_clearHistoryLiveData, response)
        }
    }

    fun leaveChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.leaveChannel(channel)
            notifyResponseAndPageState(_leaveChannelLiveData, response)
        }
    }

    fun blockAndLeaveChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.blockAndLeaveChannel(channel)
            notifyResponseAndPageState(_leaveChannelLiveData, response)
        }
    }

    fun deleteChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.deleteChannel(channel)
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

    fun muteChannel(channel: SceytChannel, muteUntil: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.muteChannel(channel, muteUntil)
            notifyResponseAndPageState(_muteUnMuteLiveData, response)
        }
    }

    fun unMuteChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsMiddleWare.unMuteChannel(channel)
            notifyResponseAndPageState(_muteUnMuteLiveData, response)
        }
    }
}