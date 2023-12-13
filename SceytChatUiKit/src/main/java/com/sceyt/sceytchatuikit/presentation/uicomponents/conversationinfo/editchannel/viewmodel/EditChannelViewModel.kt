package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.editchannel.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.EditChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.extensions.asLiveData
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class EditChannelViewModel : BaseViewModel(), SceytKoinComponent {
    private val channelMiddleWare by inject<PersistenceChanelMiddleWare>()
    private val debounceHelper by lazy { DebounceHelper(200, viewModelScope) }

    private val _editChannelLiveData = MutableLiveData<SceytChannel>()
    val editChannelLiveData: LiveData<SceytChannel> = _editChannelLiveData

    private val _isValidUrlLiveData = MutableLiveData<Pair<Boolean, String>>()
    val isValidUrlLiveData = _isValidUrlLiveData.asLiveData()


    fun editChannelChanges(channelId: Long, data: EditChannelData) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.editChannel(channelId, data)
            notifyResponseAndPageState(_editChannelLiveData, response)
        }
    }

    fun checkIsValidUrl(url: String) {
        debounceHelper.submit {
            viewModelScope.launch(Dispatchers.IO) {
                val response = channelMiddleWare.getChannelFromServerByUrl(url)
                if (response is SceytResponse.Success) {
                    _isValidUrlLiveData.postValue(response.data.isNullOrEmpty() to url)
                }
            }
        }
    }
}