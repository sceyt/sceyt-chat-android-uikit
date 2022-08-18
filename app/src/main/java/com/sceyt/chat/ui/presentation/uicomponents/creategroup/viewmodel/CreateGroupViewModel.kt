package com.sceyt.chat.ui.presentation.uicomponents.creategroup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.models.channels.CreateChannelData
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.persistence.PersistenceChanelMiddleWare
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CreateGroupViewModel : BaseViewModel(), KoinComponent {
    private val middleWare: PersistenceChanelMiddleWare by inject()

    private val _createChannelLiveData = MutableLiveData<SceytChannel>()
    val createChannelLiveData: LiveData<SceytChannel> = _createChannelLiveData

    fun createChannel(createChannelData: CreateChannelData) {
        notifyPageLoadingState(false)
        viewModelScope.launch(Dispatchers.IO) {
            val response = middleWare.createChannel(createChannelData)
            notifyResponseAndPageState(_createChannelLiveData, response)
        }
    }
}