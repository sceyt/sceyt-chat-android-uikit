package com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.toMessage
import com.sceyt.chat.ui.extensions.customToastSnackBar
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.MessageInputView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


fun MessageListViewModel.bindView(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {

    lifecycleOwner.lifecycleScope.launch {
        messagesFlow.collect {
            when (it) {
                is SceytResponse.Success -> {
                    it.data?.let { data -> messagesListView.setMessagesList(data) }
                }
                is SceytResponse.Error -> {
                    customToastSnackBar(messagesListView, it.message ?: "")
                }
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        loadMoreMessagesFlow.collect {
            if (it is SceytResponse.Success && it.data != null)
                messagesListView.addNextPageMessages(it.data)
        }
    }

    messageEditedDeletedLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                it.data?.let { data -> messagesListView.updateMessage(data, true) }
            }
            is SceytResponse.Error -> {
                customToastSnackBar(messagesListView, it.message ?: "")
            }
        }
    }

    addDeleteReactionLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                it.data?.let { data -> messagesListView.updateReaction(data) }
            }
            is SceytResponse.Error -> {
                customToastSnackBar(messagesListView, it.message ?: "")
            }
        }
    }

    onNewMessageLiveData.observe(lifecycleOwner) {
        val initMessage = mapToMessageListItem(
            data = arrayListOf(it),
            hasNext = false,
            lastMessage = messagesListView.getLastMessage()).map { item ->
            item as MessageListItem.MessageItem
        }
        messagesListView.addNewMessages(*initMessage.toTypedArray())
        messagesListView.updateViewState(BaseViewModel.PageState(isEmpty = false))
    }

    onMessageStatusLiveData.observe(lifecycleOwner) {
        messagesListView.updateMessagesStatus(it.status, it.messageIds)
    }

    messageSentLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                it.data?.let { sceytUiMessage ->
                    val message = setMessageDateAndState(sceytUiMessage, messagesListView.getLastMessage()?.message)
                    messagesListView.updateMessage(message, false)
                }
            }
            is SceytResponse.Error -> {
                it.data?.let { msg ->
                    messagesListView.messageSendFailed(msg.id)
                }
                customToastSnackBar(messagesListView, it.message ?: "")
            }
        }
    }

    onMessageReactionUpdatedLiveData.observe(lifecycleOwner) {
        messagesListView.updateReaction(it)
    }

    onMessageEditedOrDeletedLiveData.observe(lifecycleOwner) {
        val message = setMessageDateAndState(it, messagesListView.getLastMessage()?.message)
        messagesListView.updateMessage(message, true)
    }

    onChannelHistoryClearedLiveData.observe(lifecycleOwner) {
        messagesListView.clearData()
    }

    pageStateLiveData.observe(lifecycleOwner) {
        messagesListView.updateViewState(it)
    }

    messagesListView.setMessageEventListener {
        onMessageEvent(it)
    }

    messagesListView.setMessageReactionsEventListener {
        onReactionEvent(it)
    }

    messagesListView.setNeedLoadMoreMessagesListener { _, message ->
        if (!isLoadingMessages && hasNext) {
            isLoadingMessages = true
            val lastMessageId = (message as? MessageListItem.MessageItem)?.message?.id ?: 0
            loadMessages(lastMessageId, true)
        }
    }
}

fun MessageListViewModel.bindMessageInputView(messageInputView: MessageInputView,
                                              lifecycleOwner: LifecycleOwner) {

    onEditMessageCommandLiveData.observe(lifecycleOwner) {
        messageInputView.message = it.toMessage()
    }

    onReplayMessageCommandLiveData.observe(lifecycleOwner) {
        messageInputView.replayMessage(it.toMessage())
    }

    messageInputView.messageBoxActionCallback = object : MessageInputView.MessageBoxActionCallback {
        override fun sendMessage(message: Message) {
            messageInputView.cancelReplay {
                this@bindMessageInputView.sendMessage(message)
            }
        }

        override fun sendReplayMessage(message: Message, parent: Message?) {
            messageInputView.cancelReplay {
                this@bindMessageInputView.sendReplayMessage(message, parent)
            }
        }

        override fun editMessage(message: Message) {
            this@bindMessageInputView.editMessage(message)
            messageInputView.cancelReplay()
        }

        override fun addAttachments() {

        }
    }
}


/*
fun bindViewFromJava(viewModel: ChannelsViewModel, channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bindView(channelsListView, lifecycleOwner)
}
*/
