package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chatuikit.data.models.channels.SceytChannel

class MessageListViewModelFactory(
    private val channel: SceytChannel,
    private val targetMessageId: Long? = null,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val conversationId = channel.id

        @Suppress("UNCHECKED_CAST")
        return MessageListViewModel(
            conversationId = conversationId,
            channel = channel,
            replyInThread = false,
            initialTargetMessageId = targetMessageId,
        ) as T
    }
}
