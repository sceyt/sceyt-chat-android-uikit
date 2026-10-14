package com.sceyt.chatuikit.presentation.components.pinned_messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chatuikit.data.models.channels.SceytChannel

class PinnedMessagesViewModelFactory(
    private val channel: SceytChannel,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PinnedMessagesViewModel(channel) as T
    }
}
