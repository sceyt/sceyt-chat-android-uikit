package com.sceyt.chatuikit.presentation.components.channel_info.edit.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class EditChannelViewModel : BaseViewModel(), SceytKoinComponent {
    private val channelInteractor by inject<ChannelInteractor>()
    private val debounceHelper by lazy { DebounceHelper(200, viewModelScope) }

    private val _editChannelLiveData = MutableLiveData<SceytChannel>()
    val editChannelLiveData: LiveData<SceytChannel> = _editChannelLiveData

    private val _isValidUrlLiveData = MutableLiveData<Pair<Boolean, String>>()
    val isValidUrlLiveData = _isValidUrlLiveData.asLiveData()


    fun editChannelChanges(channelId: Long, data: EditChannelData) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.editChannel(channelId, data)
            notifyResponseAndPageState(_editChannelLiveData, response)
        }
    }

    fun checkIsValidUrl(url: String) {
        debounceHelper.submit {
            viewModelScope.launch(Dispatchers.IO) {
                val response = channelInteractor.getChannelFromServerByUrl(url)
                if (response is SceytResponse.Success) {
                    _isValidUrlLiveData.postValue(response.data.isNullOrEmpty() to url)
                }
            }
        }
    }
}