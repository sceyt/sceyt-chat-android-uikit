package com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.extensions.customToastSnackBar
import com.sceyt.chat.ui.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.MessageListItem
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


fun MessageListViewModel.bindView(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {

    lifecycleOwner.lifecycleScope.launch {
        messagesFlow.collect {
            if (it is SceytResponse.Success) {
                it.data?.let { data -> messagesListView.setMessagesList(data) }
            } else if (it is SceytResponse.Error) {
                customToastSnackBar(messagesListView, it.message ?: "")
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        loadMoreMessagesFlow.collect {
            if (it is SceytResponse.Success && it.data != null)
                messagesListView.addNewMessages(it.data)
        }
    }

    pageStateLiveData.observe(lifecycleOwner) {
        messagesListView.updateState(it)
    }

    messagesListView.setReachToStartListener { _, message ->
        if (!isLoadingMessages && hasNext) {
            isLoadingMessages = true
            val lastMessageId = (message as? MessageListItem.MessageItem)?.message?.id ?: 0
            loadMessages(lastMessageId, true)
        }
    }
}


/*

fun bindViewFromJava(viewModel: ChannelsViewModel, channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bindView(channelsListView, lifecycleOwner)
}
*/
