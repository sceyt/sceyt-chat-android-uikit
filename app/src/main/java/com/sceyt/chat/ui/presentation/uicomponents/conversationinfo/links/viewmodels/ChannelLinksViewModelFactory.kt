package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.ui.data.models.channels.SceytChannel

class ChannelLinksViewModelFactory(private val channel: SceytChannel) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val conversationId = channel.id
        @Suppress("UNCHECKED_CAST")
        return LinksViewModel(conversationId, channel) as T
    }
}