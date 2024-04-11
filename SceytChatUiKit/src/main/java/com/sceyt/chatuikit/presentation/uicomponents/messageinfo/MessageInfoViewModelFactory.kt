package com.sceyt.chatuikit.presentation.uicomponents.messageinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chatuikit.data.models.messages.SceytMessage

class MessageInfoViewModelFactory(private val message: SceytMessage) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MessageInfoViewModel(message.copy()) as T
    }
}