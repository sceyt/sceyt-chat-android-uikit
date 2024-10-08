package com.sceyt.chatuikit.presentation.components.message_info.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MessageInfoViewModelFactory(private val messageId: Long,
                                  private val channelId: Long) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MessageInfoViewModel(messageId, channelId) as T
    }
}