package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.databinding.*
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.LoadingViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

open class MessageViewHolderFactory(context: Context) {

    private var listeners = MessageClickListenersImpl()
    private val layoutInflater = LayoutInflater.from(context)
    private val viewPoolReactions = RecyclerView.RecycledViewPool()
    private val viewPoolFiles = RecyclerView.RecycledViewPool()


    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MessageListItem> {
        return when (viewType) {
            MessageTypeEnum.IncText.ordinal -> {
                IncTextMsgViewHolder(
                    SceytItemIncTextMessageBinding.inflate(layoutInflater, parent, false),
                    viewPoolReactions, listeners)
            }
            MessageTypeEnum.OutText.ordinal -> {
                OutTextMsgViewHolder(SceytItemOutTextMessageBinding.inflate(layoutInflater, parent, false),
                    viewPoolReactions, listeners)
            }
            MessageTypeEnum.IncFiles.ordinal -> {
                IncFilesMsgViewHolder(
                    SceytItemIncFilesMessageBinding.inflate(layoutInflater, parent, false),
                    viewPoolReactions,
                    viewPoolFiles, listeners)
            }
            MessageTypeEnum.OutFiles.ordinal -> {
                OutFilesMsgViewHolder(
                    SceytItemOutFilesMessageBinding.inflate(layoutInflater, parent, false),
                    viewPoolReactions,
                    viewPoolFiles, listeners)
            }
            MessageTypeEnum.IncDeleted.ordinal -> {
                IncDeletedMsgViewHolder(
                    SceytItemIncDeletedMessageBinding.inflate(layoutInflater, parent, false),
                    listeners)
            }
            MessageTypeEnum.OutDeleted.ordinal -> {
                OutDeletedMsgViewHolder(
                    SceytItemOutDeletedMessageBinding.inflate(layoutInflater, parent, false),
                    listeners)
            }
            MessageTypeEnum.Loading.ordinal -> LoadingViewHolder(
                SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
            )
            else -> throw Exception("Not supported view type")
        }
    }

    open fun getItemViewType(item: MessageListItem): Int {
        return when (item) {
            is MessageListItem.MessageItem -> getMessageType(item.message).ordinal
            is MessageListItem.LoadingMoreItem -> return MessageTypeEnum.Loading.ordinal
        }
    }

    private fun getMessageType(message: SceytMessage): MessageTypeEnum {
        val inc = message.incoming
        return when {
            message.state == MessageState.Deleted -> if (inc) MessageTypeEnum.IncDeleted else MessageTypeEnum.OutDeleted
            message.attachments.isNullOrEmpty() -> if (inc) MessageTypeEnum.IncText else MessageTypeEnum.OutText
            else -> if (inc) MessageTypeEnum.IncFiles else MessageTypeEnum.OutFiles
        }
    }

    fun setMessageListener(listener: MessageClickListeners) {
        listeners.setListener(listener)
    }

    enum class MessageTypeEnum {
        Loading,
        IncText,
        OutText,
        IncDeleted,
        OutDeleted,
        IncFiles,
        OutFiles
    }
}