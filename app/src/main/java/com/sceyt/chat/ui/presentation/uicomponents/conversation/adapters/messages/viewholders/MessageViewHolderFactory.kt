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
    private val viewPool = RecyclerView.RecycledViewPool()


    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MessageListItem> {
        return when (viewType) {
            MessageTypeEnum.IncText.ordinal -> {
                IncTextMsgViewHolder(SceytUiItemIncTextMessage2Binding.inflate(layoutInflater, parent, false), viewPool)
            }
            MessageTypeEnum.OutText.ordinal -> {
                OutTextMsgViewHolder(SceytUiItemOutTextMessage2Binding.inflate(layoutInflater, parent, false), viewPool)
            }
            MessageTypeEnum.Loading.ordinal -> LoadingViewHolder(
                SceytUiItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
            )
            // else -> throw Exception("Not supported view type")
            else -> {
                IncTextMsgViewHolder(SceytUiItemIncTextMessage2Binding.inflate(layoutInflater, parent, false), viewPool)
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
            message.attachments?.size == 1 && message.attachments?.getOrNull(0)?.type.isEqualsVideoOrImage() ->
                if (inc) MessageTypeEnum.IncSingleVideoOrImage else MessageTypeEnum.IncSingleVideoOrImage
            else -> if (inc) MessageTypeEnum.IncAttachments else MessageTypeEnum.IncAttachments
        }
    }

    internal enum class MessageTypeEnum {
        Loading,
        IncText,
        OutText,
        IncDeleted,
        IncSingleVideoOrImage,
        IncAttachments;
    }
}