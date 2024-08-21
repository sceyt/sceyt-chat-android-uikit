package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewStub
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemOutFileMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutImageMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutLinkMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutTextMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutVideoMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutVoiceMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageViewHolderFactory.MessageViewTypeEnum
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutFileMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutImageMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutLinkMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutTextMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutVideoMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutVoiceMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle

open class MessageInfoViewProvider(private val context: Context) {
    protected val viewPoolReactions = RecyclerView.RecycledViewPool()
    protected var clickListeners = MessageClickListenersImpl()
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    protected var userNameFormatter: UserNameFormatter? = SceytChatUIKit.userNameFormatter
    private var needMediaDataCallback: (NeedMediaInfoData) -> Unit = {}
    protected var viewHolder: BaseMsgViewHolder? = null
    protected val messageItemStyle: MessageItemStyle by lazy { getMessageListViewStyle() }
    var viewType: Int = 0
        private set

    open fun displayMessagePreview(viewStub: ViewStub, message: SceytMessage) {
        val holder = createViewHolderWithMessage(viewStub, message)
        holder.bind(MessageListItem.MessageItem(message), MessageDiff.DEFAULT)
    }

    open fun createViewHolderWithMessage(viewStub: ViewStub, message: SceytMessage): BaseMsgViewHolder {
        viewType = getMessageType(message)
        return when (viewType) {
            MessageViewTypeEnum.OutText.ordinal -> createOutTextMsgViewHolder(viewStub, R.layout.sceyt_item_out_text_message)
            MessageViewTypeEnum.OutLink.ordinal -> createOutLinkMsgViewHolder(viewStub, R.layout.sceyt_item_out_link_message)
            MessageViewTypeEnum.OutVoice.ordinal -> createOutVoiceMsgViewHolder(viewStub, R.layout.sceyt_item_out_voice_message)
            MessageViewTypeEnum.OutImage.ordinal -> createOutImageMsgViewHolder(viewStub, R.layout.sceyt_item_out_image_message)
            MessageViewTypeEnum.OutVideo.ordinal -> createOutVideoMsgViewHolder(viewStub, R.layout.sceyt_item_out_video_message)
            MessageViewTypeEnum.OutFile.ordinal -> createOutFileMsgViewHolder(viewStub, R.layout.sceyt_item_out_file_message)
            else -> throw RuntimeException("Not supported view type")
        }.also { viewHolder = it }
    }

    private fun createOutTextMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMsgViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutTextMessageBinding.bind(viewStub.inflate())
        return OutTextMsgViewHolder(binding, viewPoolReactions,
            messageItemStyle, clickListeners, userNameFormatter)
    }

    private fun createOutLinkMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMsgViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutLinkMessageBinding.bind(viewStub.inflate())
        return OutLinkMsgViewHolder(binding, viewPoolReactions,
            messageItemStyle, clickListeners, userNameFormatter, needMediaDataCallback)
    }

    private fun createOutVoiceMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMsgViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutVoiceMessageBinding.bind(viewStub.inflate())
        return OutVoiceMsgViewHolder(binding, viewPoolReactions, messageItemStyle, clickListeners, userNameFormatter,
            needMediaDataCallback, null)
    }

    private fun createOutImageMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMsgViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutImageMessageBinding.bind(viewStub.inflate())
        return OutImageMsgViewHolder(binding, viewPoolReactions, messageItemStyle,
            clickListeners, userNameFormatter, needMediaDataCallback)
    }

    private fun createOutVideoMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMsgViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutVideoMessageBinding.bind(viewStub.inflate())
        return OutVideoMsgViewHolder(binding, viewPoolReactions, messageItemStyle,
            clickListeners, userNameFormatter, needMediaDataCallback)
    }

    private fun createOutFileMsgViewHolder(viewStub: ViewStub, layoutId: Int): BaseMsgViewHolder {
        viewStub.layoutResource = layoutId
        val binding = SceytItemOutFileMessageBinding.bind(viewStub.inflate())
        return OutFileMsgViewHolder(binding, viewPoolReactions, messageItemStyle,
            clickListeners, userNameFormatter, needMediaDataCallback)
    }

    open fun getMessageType(message: SceytMessage): Int {
        val inc = message.incoming
        val attachments = message.attachments
        val type = when {
            message.state == MessageState.Deleted -> if (inc) MessageViewTypeEnum.IncDeleted else MessageViewTypeEnum.OutDeleted
            !attachments.isNullOrEmpty() -> {
                val (links, others) = attachments.partition { it.type == AttachmentTypeEnum.Link.value() }
                // Check maybe all attachments are links
                if (links.size == attachments.size)
                    return (if (inc) MessageViewTypeEnum.IncLink else MessageViewTypeEnum.OutLink).ordinal

                val attachment = others.getOrNull(0)
                when (attachment?.type) {
                    AttachmentTypeEnum.Image.value() -> if (inc) MessageViewTypeEnum.IncImage else MessageViewTypeEnum.OutImage
                    AttachmentTypeEnum.Video.value() -> if (inc) MessageViewTypeEnum.IncVideo else MessageViewTypeEnum.OutVideo
                    AttachmentTypeEnum.File.value() -> if (inc) MessageViewTypeEnum.IncFile else MessageViewTypeEnum.OutFile
                    AttachmentTypeEnum.Voice.value() -> if (inc) MessageViewTypeEnum.IncVoice else MessageViewTypeEnum.OutVoice
                    else -> if (inc) MessageViewTypeEnum.IncFiles else MessageViewTypeEnum.OutFiles
                }
            }

            else -> if (inc) MessageViewTypeEnum.IncText else MessageViewTypeEnum.OutText
        }
        return type.ordinal
    }


    protected open fun getMessageListViewStyle(): MessageItemStyle {
        return MessagesListViewStyle.currentStyle?.messageItemStyle
                ?: MessagesListViewStyle.Builder(context = context, null).build().messageItemStyle
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