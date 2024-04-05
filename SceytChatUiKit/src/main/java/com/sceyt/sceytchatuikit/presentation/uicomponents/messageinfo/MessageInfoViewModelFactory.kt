package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

class MessageInfoViewModelFactory(private val message: SceytMessage) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MessageInfoViewModel(message.copy()) as T
    }
}