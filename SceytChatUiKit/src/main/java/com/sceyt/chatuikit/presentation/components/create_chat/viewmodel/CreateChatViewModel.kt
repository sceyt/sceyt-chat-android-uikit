package com.sceyt.chatuikit.presentation.components.create_chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class CreateChatViewModel : BaseViewModel(), SceytKoinComponent {
    private val channelInteractor by lazy { SceytChatUIKit.chatUIFacade.channelInteractor }
    private val channelMemberInteractor by lazy { SceytChatUIKit.chatUIFacade.channelMemberInteractor }
    private val systemMessageSender by inject<SystemMessageSender>()

    private val _createChatLiveData = MutableLiveData<SceytChannel>()
    val createChatLiveData: LiveData<SceytChannel> = _createChatLiveData

    private val _addMembersLiveData = MutableLiveData<SceytChannel>()
    val addMembersLiveData: LiveData<SceytChannel> = _addMembersLiveData

    private val _isValidUriLiveData = MutableLiveData<Boolean>()
    val isValidUrlLiveData: LiveData<Boolean> = _isValidUriLiveData

    fun createGroup(createChannelData: CreateChannelData) {
        createChatImpl(createChannelData, CreatedSystemMessage.Group)
    }

    fun createChannel(createChannelData: CreateChannelData) {
        createChatImpl(createChannelData, CreatedSystemMessage.Channel)
    }

    private fun createChatImpl(
        createChannelData: CreateChannelData,
        createdSystemMessage: CreatedSystemMessage?
    ) {
        notifyPageLoadingState(false)
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.createChannel(createChannelData)
            response.onSuccessNotNull { channel ->
                if (createdSystemMessage != null) {
                    sendCreatedSystemMessage(channel.id, createdSystemMessage)
                }
            }
            notifyResponseAndPageState(_createChatLiveData, response)
        }
    }

    private suspend fun sendCreatedSystemMessage(
        channelId: Long,
        systemMessage: CreatedSystemMessage
    ) {
        runCatching {
            when (systemMessage) {
                CreatedSystemMessage.Group -> systemMessageSender.sendGroupCreated(channelId)
                CreatedSystemMessage.Channel -> systemMessageSender.sendChannelCreated(channelId)
            }
        }.onFailure {
            SceytLog.e(TAG, "Failed to send created system message: ${it.message}")
        }
    }

    fun addMembers(channelId: Long, members: List<SceytMember>) {
        notifyPageLoadingState(false)
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMemberInteractor.addMembersToChannel(channelId, members)
            notifyResponseAndPageState(_addMembersLiveData, response)
        }
    }

    fun checkIsValidUri(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.getChannelFromServerByUri(uri)
            if (response is SceytResponse.Success) {
                _isValidUriLiveData.postValue(response.data == null)
            }
        }
    }

    fun checkIsValidUrlFormat(url: String): URIValidation {
        val config = SceytChatUIKit.config.channelURIConfig
        return when {
            url.length < config.minLength -> URIValidation.TooShort
            url.length > config.maxLength -> URIValidation.TooLong
            !config.regex.toPattern().matcher(url).matches() -> URIValidation.InvalidCharacters
            else -> URIValidation.Valid
        }
    }
}

enum class URIValidation {
    Valid,
    TooShort,
    TooLong,
    InvalidCharacters
}

private enum class CreatedSystemMessage {
    Group,
    Channel
}

private val TAG: String = CreateChatViewModel::class.java.name
