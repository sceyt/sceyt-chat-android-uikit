package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.databinding.*
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

open class MessageViewHolderFactory(context: Context) {

    private var listeners = MessageClickListenersImpl()
    private val layoutInflater = LayoutInflater.from(context)
    private val viewPoolReactions = RecyclerView.RecycledViewPool()
    private val viewPoolFiles = RecyclerView.RecycledViewPool()

    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseMsgViewHolder {
        return when (viewType) {
            MessageTypeEnum.IncText.ordinal -> createIncTextMsgViewHolder(parent)
            MessageTypeEnum.OutText.ordinal -> createOutTextMsgViewHolder(parent)
            MessageTypeEnum.IncFiles.ordinal -> createIncFilesMsgViewHolder(parent)
            MessageTypeEnum.OutFiles.ordinal -> createOutFilesMsgViewHolder(parent)
            MessageTypeEnum.IncDeleted.ordinal -> createIncDeletedMsgViewHolder(parent)
            MessageTypeEnum.OutDeleted.ordinal -> createOutDeletedMsgViewHolder(parent)
            MessageTypeEnum.DateSeparator.ordinal -> createDateSeparatorViewHolder(parent)
            MessageTypeEnum.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createIncTextMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncTextMsgViewHolder(
            SceytItemIncTextMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, listeners)
    }

    open fun createOutTextMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutTextMsgViewHolder(
            SceytItemOutTextMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, listeners)
    }

    open fun createIncFilesMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncFilesMsgViewHolder(
            SceytItemIncFilesMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions,
            viewPoolFiles, listeners)
    }

    open fun createOutFilesMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutFilesMsgViewHolder(
            SceytItemOutFilesMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions,
            viewPoolFiles, listeners)
    }

    open fun createIncDeletedMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncDeletedMsgViewHolder(
            SceytItemIncDeletedMessageBinding.inflate(layoutInflater, parent, false))
    }

    open fun createOutDeletedMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutDeletedMsgViewHolder(
            SceytItemOutDeletedMessageBinding.inflate(layoutInflater, parent, false))
    }

    open fun createDateSeparatorViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return DateSeparatorViewHolder(
            SceytItemMessageDateSeparatorBinding.inflate(layoutInflater, parent, false)
        )
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return LoadingMoreMessagesViewHolder(
            SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        )
    }

    open fun getItemViewType(item: MessageListItem): Int {
        return when (item) {
            is MessageListItem.MessageItem -> getMessageType(item.message).ordinal
            is MessageListItem.DateSeparatorItem -> return MessageTypeEnum.DateSeparator.ordinal
            is MessageListItem.LoadingMoreItem -> return MessageTypeEnum.Loading.ordinal
        }
    }

    open fun getMessageType(message: SceytMessage): MessageTypeEnum {
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

    protected val clickListeners get() = listeners as MessageClickListeners.ClickListeners

    enum class MessageTypeEnum {
        IncText,
        OutText,
        IncDeleted,
        OutDeleted,
        IncFiles,
        OutFiles,
        DateSeparator,
        Loading
    }
}