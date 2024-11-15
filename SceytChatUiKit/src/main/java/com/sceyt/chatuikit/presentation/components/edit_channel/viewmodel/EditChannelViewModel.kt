package com.sceyt.chatuikit.presentation.components.edit_channel.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.create_chat.viewmodel.URIValidation
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class EditChannelViewModel : BaseViewModel(), SceytKoinComponent {
    private val channelInteractor by inject<ChannelInteractor>()
    private val debounceHelper by lazy { DebounceHelper(200, viewModelScope) }

    private val _editChannelLiveData = MutableLiveData<SceytChannel>()
    val editChannelLiveData: LiveData<SceytChannel> = _editChannelLiveData

    private val _isValidUriLiveData = MutableLiveData<Pair<Boolean, String>>()
    val isValidUrlLiveData = _isValidUriLiveData.asLiveData()


    fun editChannelChanges(channelId: Long, data: EditChannelData) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.editChannel(channelId, data)
            notifyResponseAndPageState(_editChannelLiveData, response)
        }
    }

    fun checkIsValidUri(uri: String) {
        debounceHelper.submit {
            viewModelScope.launch(Dispatchers.IO) {
                val response = channelInteractor.getChannelFromServerByUri(uri)
                if (response is SceytResponse.Success) {
                    _isValidUriLiveData.postValue((response.data == null) to uri)
                }
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