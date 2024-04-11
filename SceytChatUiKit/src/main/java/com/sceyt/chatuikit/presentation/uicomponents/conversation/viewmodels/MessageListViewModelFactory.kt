package com.sceyt.chatuikit.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chatuikit.data.models.channels.SceytChannel

class MessageListViewModelFactory(private val channel: SceytChannel) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val conversationId = channel.id

        @Suppress("UNCHECKED_CAST")
        return MessageListViewModel(conversationId, false, channel) as T
    }
}