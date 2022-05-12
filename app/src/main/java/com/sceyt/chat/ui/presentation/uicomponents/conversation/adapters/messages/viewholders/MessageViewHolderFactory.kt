package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.databinding.*
import com.sceyt.chat.ui.extensions.isEqualsVideoOrImage
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.LoadingViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelsListenersImpl
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem

class MessageViewHolderFactory(context: Context) {

    private var listeners = ChannelsListenersImpl()
    private val layoutInflater = LayoutInflater.from(context)
    private val viewPoolReactions = RecyclerView.RecycledViewPool()
    private val viewPoolFiles = RecyclerView.RecycledViewPool()


    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MessageListItem> {
        return when (viewType) {
            MessageTypeEnum.IncText.ordinal -> {
                IncTextMsgViewHolder(
                    SceytUiItemIncTextMessageBinding.inflate(layoutInflater, parent, false),
                    viewPoolReactions)
            }
            MessageTypeEnum.OutText.ordinal -> {
                OutTextMsgViewHolder(SceytUiItemOutTextMessageBinding.inflate(layoutInflater, parent, false),
                    viewPoolReactions)
            }
            MessageTypeEnum.IncFiles.ordinal -> {
                IncFilesMsgViewHolder(
                    SceytUiItemIncFilesMessageBinding.inflate(layoutInflater, parent, false),
                    viewPoolReactions,
                    viewPoolFiles)
            }
            MessageTypeEnum.OutFiles.ordinal -> {
                OutFilesMsgViewHolder(
                    SceytUiItemOutFilesMessageBinding.inflate(layoutInflater, parent, false),
                    viewPoolReactions,
                    viewPoolFiles)
            }
            MessageTypeEnum.Loading.ordinal -> LoadingViewHolder(
                SceytUiItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
            )
            // else -> throw Exception("Not supported view type")
            else -> {
                IncTextMsgViewHolder(SceytUiItemIncTextMessageBinding.inflate(layoutInflater, parent, false), viewPoolReactions)
            }
        }
    }

    fun setChannelListener(listener: ChannelListeners) {
        listeners.setListener(listener)
    }

    fun getItemViewType(item: MessageListItem): Int {
        return when (item) {
            is MessageListItem.MessageItem -> getMessageType(item.message).ordinal
            is MessageListItem.LoadingMoreItem -> return MessageTypeEnum.Loading.ordinal
        }
    }

    private fun getMessageType(message: SceytUiMessage): MessageTypeEnum {
        val inc = message.incoming
        return when {
            message.state == MessageState.Deleted -> if (inc) MessageTypeEnum.IncDeleted else MessageTypeEnum.IncDeleted
            message.attachments.isNullOrEmpty() -> if (inc) MessageTypeEnum.IncText else MessageTypeEnum.OutText
            //todo
            message.attachments?.size == 1 && message.attachments?.getOrNull(0)?.type.isEqualsVideoOrImage() ->
                if (inc) MessageTypeEnum.IncFiles else MessageTypeEnum.OutFiles
            else -> if (inc) MessageTypeEnum.IncFiles else MessageTypeEnum.OutFiles
        }
    }

    internal enum class MessageTypeEnum {
        Loading,
        IncText,
        OutText,
        IncDeleted,
        OutDeleted,
        IncSingleVideoOrImage,
        IncFiles,
        OutFiles;
    }
}