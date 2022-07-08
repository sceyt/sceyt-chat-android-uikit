package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.ChannelsRepository
import com.sceyt.chat.ui.data.ChannelsRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.toChannel
import com.sceyt.chat.ui.data.toGroupChannel
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConversationInfoViewModel : BaseViewModel() {
    // Todo di
    private val repo: ChannelsRepository = ChannelsRepositoryImpl()

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


    fun getChannel(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.getChannel(id)
            notifyResponseAndPageState(_channelLiveData, response)
        }
    }

    fun saveChanges(channel: SceytChannel, newSubject: String, avatarUrl: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            var newUrl = avatarUrl
            val editedAvatar = channel.getChannelAvatarUrl() != avatarUrl
            if (editedAvatar && avatarUrl != null) {
                val uploadResult = repo.uploadAvatar(avatarUrl)
                if (uploadResult is SceytResponse.Success) {
                    newUrl = uploadResult.data
                } else {
                    notifyPageStateWithResponse(uploadResult)
                    return@launch
                }
            }
            val response = repo.editChannel(channel.toChannel(), newSubject, newUrl)
            notifyResponseAndPageState(_editChannelLiveData, response)
        }
    }

    fun clearHistory(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.clearHistory(channel.toChannel())
            notifyResponseAndPageState(_clearHistoryLiveData, response)
        }
    }

    fun leaveChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.leaveChannel(channel.toGroupChannel())
            notifyResponseAndPageState(_leaveChannelLiveData, response)
        }
    }

    fun blockAndLeaveChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.blockChannel(channel.toGroupChannel())
            notifyResponseAndPageState(_leaveChannelLiveData, response)
        }
    }

    fun deleteChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.deleteChannel(channel.toChannel())
            notifyResponseAndPageState(_deleteChannelLiveData, response)
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.blockUser(userId)
            notifyResponseAndPageState(_blockUnblockUserLiveData, response)
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.unblockUser(userId)
            notifyResponseAndPageState(_blockUnblockUserLiveData, response)
        }
    }

    fun muteChannel(channel: SceytChannel, muteUntil: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.muteChannel(channel.toChannel(), muteUntil)
            notifyResponseAndPageState(_muteUnMuteLiveData, response)
        }
    }

    fun unMuteChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.unMuteChannel(channel.toChannel())
            notifyResponseAndPageState(_muteUnMuteLiveData, response)
        }
    }
}