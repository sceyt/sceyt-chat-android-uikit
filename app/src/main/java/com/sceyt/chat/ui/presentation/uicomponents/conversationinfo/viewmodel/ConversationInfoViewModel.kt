package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.ChannelsRepository
import com.sceyt.chat.ui.data.ChannelsRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.toChannel
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConversationInfoViewModel : BaseViewModel() {
    // Todo di
    private val repo: ChannelsRepository = ChannelsRepositoryImpl()

    private val _editChannelLiveData = MutableLiveData<SceytChannel>()
    val editChannelLiveData: LiveData<SceytChannel> = _editChannelLiveData

    fun saveChanges(channel: SceytChannel, newSubject: String, avatarUrl: String?, editedAvatar: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            var newUrl = avatarUrl
            if (editedAvatar && avatarUrl != null) {
                val uploadResult = repo.uploadAvatar(avatarUrl)
                if (uploadResult is SceytResponse.Success) {
                    newUrl = uploadResult.data
                } else {
                    notifyPageStateWithResponse(uploadResult)
                    return@launch
                }
            }
            when (val response = repo.editChannel(channel.toChannel(), newSubject, newUrl)) {
                is SceytResponse.Success -> {
                    _editChannelLiveData.postValue(response.data)
                }
                is SceytResponse.Error -> {
                    notifyPageStateWithResponse(response)
                }
            }
        }
    }
}