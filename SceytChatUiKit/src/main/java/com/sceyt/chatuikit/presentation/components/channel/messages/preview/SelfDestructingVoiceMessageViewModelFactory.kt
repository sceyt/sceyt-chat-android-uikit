package com.sceyt.chatuikit.presentation.components.channel.messages.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chatuikit.data.models.messages.SceytMessage

class SelfDestructingVoiceMessageViewModelFactory(
    private val message: SceytMessage
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SelfDestructingVoiceMessageViewModel(message) as T
    }
}