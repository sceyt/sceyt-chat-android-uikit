package com.sceyt.chat.demo.presentation.createconversation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.SceytKitClient
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.toMember
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreateChatViewModel : BaseViewModel() {
    private val channelMiddleWare by lazy { SceytKitClient.getChannelsMiddleWare() }
    private val membersMiddleWare by lazy { SceytKitClient.getMembersMiddleWare() }

    private val _createChatLiveData = MutableLiveData<SceytChannel>()
    val createChatLiveData: LiveData<SceytChannel> = _createChatLiveData

    private val _addMembersLiveData = MutableLiveData<SceytChannel>()
    val addMembersLiveData: LiveData<SceytChannel> = _addMembersLiveData

    private val _isValidUrlLiveData = MutableLiveData<Boolean>()
    val isValidUrlLiveData: LiveData<Boolean> = _isValidUrlLiveData

    fun createChat(createChannelData: CreateChannelData) {
        notifyPageLoadingState(false)
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.createChannel(createChannelData)
            notifyResponseAndPageState(_createChatLiveData, response)
        }
    }

    fun addMembers(channelId: Long, users: List<SceytMember>) {
        notifyPageLoadingState(false)
        viewModelScope.launch(Dispatchers.IO) {
            val members = users.map { it.toMember() }
            val response = membersMiddleWare.addMembersToChannel(channelId, members)
            notifyResponseAndPageState(_addMembersLiveData, response)
        }
    }

    fun checkIsValidUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.getChannelFromServerByUrl(url)
            if (response is SceytResponse.Success) {
                _isValidUrlLiveData.postValue(response.data.isNullOrEmpty())
            }
        }
    }
}