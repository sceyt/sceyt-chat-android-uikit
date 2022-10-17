package com.sceyt.sceytchatuikit.presentation.uicomponents.creategroup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.toMember
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class CreateChatViewModel : BaseViewModel(), SceytKoinComponent {
    private val channelMiddleWare: PersistenceChanelMiddleWare by inject()
    private val membersMiddleWare: PersistenceMembersMiddleWare by inject()
    private val messageMiddleWare: PersistenceMessagesMiddleWare by inject()

    private val _createChatLiveData = MutableLiveData<SceytChannel>()
    val createChatLiveData: LiveData<SceytChannel> = _createChatLiveData

    private val _addMembersLiveData = MutableLiveData<SceytChannel>()
    val addMembersLiveData: LiveData<SceytChannel> = _addMembersLiveData

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
}