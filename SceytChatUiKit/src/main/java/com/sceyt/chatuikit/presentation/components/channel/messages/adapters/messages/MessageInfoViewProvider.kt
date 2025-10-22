package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewStub
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemOutFileMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutImageMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutLinkMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutTextMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutUnsupportedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutVideoMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutVoiceMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageViewHolderFactory.MessageViewTypeEnum
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutFileMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutImageMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutLinkMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutTextMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutUnsupportedMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutVideoMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutVoiceMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListenersImpl
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

open class MessageInfoViewProvider(
        context: Context,
        private val messageItemStyle: MessageItemStyle
) {
    protected val viewPoolReactions = RecyclerView.RecycledViewPool()
    protected var clickListeners = MessageClickListenersImpl()
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var needMediaDataCallback: (NeedMediaInfoData) -> Unit = {}
    protected var viewHolder: BaseMessageViewHolder? = null
    var viewType: Int = 0
        private set

    open fun displayMessagePreview(viewStub: ViewStub, message: SceytMessage) {
        val holder = createViewHolderWithMessage(viewStub, message)
        holder.bind(MessageListItem.MessageItem(message), MessageDiff.DEFAULT)
    }

    open fun createViewHolderWithMessage(viewStub: ViewStub, message: SceytMessage): BaseMessageViewHolder {
        viewType = getMessageType(message)
        return when (viewType) {
            MessageViewTypeEnum.OutText.ordinal -> createOutTextMsgViewHolder(viewStub, R.layout.sceyt_item_out_text_message)
            MessageViewTypeEnum.OutLink.ordinal -> createOutLinkMsgViewHolder(viewStub, R.layout.sceyt_item_out_link_message)
            MessageViewTypeEnum.OutVoice.ordinal -> createOutVoiceMsgViewHolder(viewStub, R.layout.sceyt_item_out_voice_message)
            MessageViewTypeEnum.OutImage.ordinal -> createOutImageMsgViewHolder(viewStub, R.layout.sceyt_item_out_image_message)
            MessageViewTypeEnum.OutVideo.ordinal -> createOutVideoMsgViewHolder(viewStub, R.layout.sceyt_item_out_video_message)
            MessageViewTypeEnum.OutFile.ordinal -> createOutFileMsgViewHolder(viewStub, R.layout.sceyt_item_out_file_message)
            MessageViewTypeEnum.OutUnsupported.ordinal -> createOutUnsupportedMsgViewHolder(viewStub, R.layout.sceyt_item_out_unsupported_message)
            else -> throw RuntimeException("Not supported view type")
        }.also { viewHolder = it }
    }

    private fun createOutTextMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMessageViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutTextMessageBinding.bind(viewStub.inflate())
        return OutTextMessageViewHolder(binding, viewPoolReactions, messageItemStyle, clickListeners)
    }

    private fun createOutLinkMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMessageViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutLinkMessageBinding.bind(viewStub.inflate())
        return OutLinkMessageViewHolder(binding, viewPoolReactions,
            messageItemStyle, clickListeners, needMediaDataCallback)
    }

    private fun createOutVoiceMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMessageViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutVoiceMessageBinding.bind(viewStub.inflate())
        return OutVoiceMessageViewHolder(binding, viewPoolReactions, messageItemStyle, clickListeners,
            needMediaDataCallback, null)
    }

    private fun createOutImageMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMessageViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutImageMessageBinding.bind(viewStub.inflate())
        return OutImageMessageViewHolder(binding, viewPoolReactions, messageItemStyle,
            clickListeners, needMediaDataCallback)
    }

    private fun createOutVideoMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMessageViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutVideoMessageBinding.bind(viewStub.inflate())
        return OutVideoMessageViewHolder(binding, viewPoolReactions, messageItemStyle,
            clickListeners, needMediaDataCallback)
    }

    private fun createOutFileMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMessageViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutFileMessageBinding.bind(viewStub.inflate())
        return OutFileMessageViewHolder(binding, viewPoolReactions, messageItemStyle,
            clickListeners, needMediaDataCallback)
    }

    private fun createOutUnsupportedMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMessageViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutUnsupportedMessageBinding.bind(viewStub.inflate())
        return OutUnsupportedMessageViewHolder(binding, viewPoolReactions, messageItemStyle, clickListeners)
    }

    private fun pick(inc: Boolean, incType: MessageViewTypeEnum, outType: MessageViewTypeEnum) =
            if (inc) incType else outType

    open fun getMessageType(message: SceytMessage): Int {
        val inc = message.incoming
        val attachments = message.attachments.orEmpty()

        if (message.state == MessageState.Deleted) {
            return pick(inc, MessageViewTypeEnum.IncDeleted, MessageViewTypeEnum.OutDeleted).ordinal
        }

        return when (MessageTypeEnum.fromValue(message.type)) {
            MessageTypeEnum.Text,
            MessageTypeEnum.Media,
            MessageTypeEnum.File,
            MessageTypeEnum.Link -> resolveContentViewType(inc, attachments).ordinal

            MessageTypeEnum.System, null ->
                pick(inc, MessageViewTypeEnum.IncUnsupported, MessageViewTypeEnum.OutUnsupported).ordinal
        }
    }

    private fun resolveContentViewType(
            inc: Boolean,
            attachments: List<SceytAttachment>
    ): MessageViewTypeEnum {
        if (attachments.isEmpty()) {
            return pick(inc, MessageViewTypeEnum.IncText, MessageViewTypeEnum.OutText)
        }

        val (links, nonLinks) = attachments.partition { it.type == AttachmentTypeEnum.Link.value }

        if (links.size == attachments.size) {
            return pick(inc, MessageViewTypeEnum.IncLink, MessageViewTypeEnum.OutLink)
        }

        val first = nonLinks.first()
        return when (first.type) {
            AttachmentTypeEnum.Image.value ->
                pick(inc, MessageViewTypeEnum.IncImage, MessageViewTypeEnum.OutImage)

            AttachmentTypeEnum.Video.value ->
                pick(inc, MessageViewTypeEnum.IncVideo, MessageViewTypeEnum.OutVideo)

            AttachmentTypeEnum.File.value ->
                pick(inc, MessageViewTypeEnum.IncFile, MessageViewTypeEnum.OutFile)

            AttachmentTypeEnum.Voice.value ->
                pick(inc, MessageViewTypeEnum.IncVoice, MessageViewTypeEnum.OutVoice)

            else ->
                pick(inc, MessageViewTypeEnum.IncFiles, MessageViewTypeEnum.OutFiles)
        }
    }

    fun setMessageListener(listener: MessageClickListeners) {
        clickListeners.setListener(listener)
    }

    fun updateMessageStatus(message: SceytMessage) {
        viewHolder?.bind(MessageListItem.MessageItem(message), MessageDiff.DEFAULT_FALSE.copy(statusChanged = true))
    }

    fun updateMessage(message: SceytMessage) {
        viewHolder?.bind(MessageListItem.MessageItem(message), MessageDiff.DEFAULT)
    }

    fun setNeedMediaDataCallback(callback: (NeedMediaInfoData) -> Unit) {
        needMediaDataCallback = callback
    }

    fun getNeedMediaDataCallback(): (NeedMediaInfoData) -> Unit {
        return needMediaDataCallback
    }
}