package com.sceyt.chat.ui.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.ui.data.ChannelsRepositoryImpl
import com.sceyt.chat.ui.data.SceytResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChannelsViewModel : ViewModel() {

    // todo di
    private val repo = ChannelsRepositoryImpl()

    private val _uiState = MutableStateFlow<SceytResponse<List<Channel>>>(SceytResponse.Loading())
    val uiState: StateFlow<SceytResponse<List<Channel>>> = _uiState


    fun getChannels() {
        viewModelScope.launch {
            repo.getChannels().collect {
                _uiState.value = it
            }
        }
    }
}
