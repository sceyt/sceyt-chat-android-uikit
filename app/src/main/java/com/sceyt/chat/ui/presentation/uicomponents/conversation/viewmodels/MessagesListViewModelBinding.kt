package com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventEnum
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.toMessage
import com.sceyt.chat.ui.extensions.asAppCompatActivity
import com.sceyt.chat.ui.extensions.customToastSnackBar
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.MessageInputView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


fun MessageListViewModel.bindView(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {

    messagesListView.enableDisableClickActions(!replayInThread)

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
                it.data?.let { data -> messagesListView.messageEditedOrDeleted(data) }
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
            lastMessage = messagesListView.getLastMessage())

        messagesListView.addNewMessages(*initMessage.toTypedArray())
        messagesListView.updateViewState(BaseViewModel.PageState(isEmpty = false))

        markMessageAsDisplayed(it.id)
    }

    onNewThreadMessageLiveData.observe(lifecycleOwner) {
        messagesListView.updateReplayCount(it)
    }

    onMessageStatusLiveData.observe(lifecycleOwner) {
        messagesListView.updateMessagesStatus(it.status, it.messageIds)
    }

    messageSentLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                it.data?.let { sceytUiMessage ->
                    sceytUiMessage.canShowAvatarAndName = shouldShowAvatarAndName(sceytUiMessage, messagesListView.getLastMessage()?.message)
                    messagesListView.updateMessage(sceytUiMessage)
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
        messagesListView.messageEditedOrDeleted(it)
    }

    onChannelEventLiveData.observe(lifecycleOwner) {
        when (it.eventType) {
            ChannelEventEnum.ClearedHistory -> messagesListView.clearData()
            ChannelEventEnum.Deleted, ChannelEventEnum.Left -> {
                messagesListView.context.asAppCompatActivity()?.finish()
            }
            else -> return@observe
        }
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

fun MessageListViewModel.bindView(messageInputView: MessageInputView,
                                  replayInThreadMessage: SceytMessage?,
                                  lifecycleOwner: LifecycleOwner) {

    messageInputView.setReplayInThreadMessageId(replayInThreadMessage?.id)

    onEditMessageCommandLiveData.observe(lifecycleOwner) {
        messageInputView.message = it.toMessage()
    }

    onReplayMessageCommandLiveData.observe(lifecycleOwner) {
        messageInputView.replayMessage(it.toMessage())
    }

    messageInputView.messageInputActionCallback = object : MessageInputView.MessageInputActionCallback {
        override fun sendMessage(message: Message) {
            messageInputView.cancelReplay {
                this@bindView.sendMessage(message)
            }
        }

        override fun sendReplayMessage(message: Message, parent: Message?) {
            messageInputView.cancelReplay {
                this@bindView.sendReplayMessage(message, parent)
            }
        }

        override fun editMessage(message: Message) {
            this@bindView.editMessage(message)
            messageInputView.cancelReplay()
        }
    }
}

fun MessageListViewModel.bindView(headerView: ConversationHeaderView,
                                  replayInThreadMessage: SceytMessage?) {

    if (replayInThread)
        headerView.setReplayMessage(replayInThreadMessage)
    else
        headerView.setChannel(channel)

}


fun bindViewFromJava(viewModel: MessageListViewModel, messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bindView(messagesListView, lifecycleOwner)
}
