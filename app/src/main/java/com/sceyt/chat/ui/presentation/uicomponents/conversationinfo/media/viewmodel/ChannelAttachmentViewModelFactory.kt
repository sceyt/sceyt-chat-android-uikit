package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.ui.data.models.channels.SceytChannel

class ChannelAttachmentViewModelFactory(private val channel: SceytChannel) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val conversationId = channel.id
        @Suppress("UNCHECKED_CAST")
        return ChannelAttachmentsViewModel(conversationId, channel) as T
    }
}