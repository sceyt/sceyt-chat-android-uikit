package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemIncAttachmentsMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncDeletedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncFileMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncImageMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncLinkMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncPollMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncTextMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncUnsupportedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncVideoMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncVoiceMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.chatuikit.databinding.SceytItemOutAttachmentsMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutDeletedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutFileMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutImageMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutLinkMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutPollMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutTextMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutUnsupportedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutVideoMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutVoiceMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemUnreadMessagesSeparatorBinding
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.DateSeparatorViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncAttachmentsMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncDeletedMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncFileMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncImageMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncLinkMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncPollMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncTextMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncUnsupportedMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncVideoMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncVoiceMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.LoadingMoreMessagesViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutAttachmentsMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutDeletedMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutFileMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutImageMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutLinkMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutPollMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutTextMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutUnsupportedMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutVideoMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutVoiceMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.UnreadMessagesSeparatorViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListenersImpl
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle

open class MessageViewHolderFactory(context: Context) {
    protected val viewPoolReactions = RecyclerView.RecycledViewPool()
    protected val viewPoolFiles = RecyclerView.RecycledViewPool()
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var messageItemStyle: MessagesListViewStyle
    private var clickListeners = MessageClickListenersImpl()
    private var displayedListener: ((MessageListItem) -> Unit)? = null
    private var voicePlayPauseListener: ((FileListItem, SceytMessage, playing: Boolean) -> Unit)? = null
    private var needMediaDataCallback: (NeedMediaInfoData) -> Unit = {}

    internal fun setStyle(style: MessagesListViewStyle) {
        this.messageItemStyle = style
    }

    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseMessageViewHolder {
        return when (viewType) {
            MessageViewTypeEnum.IncText.ordinal -> createIncTextMsgViewHolder(parent)
            MessageViewTypeEnum.OutText.ordinal -> createOutTextMsgViewHolder(parent)
            MessageViewTypeEnum.OutLink.ordinal -> createOutLinkMsgViewHolder(parent)
            MessageViewTypeEnum.IncLink.ordinal -> createIncLinkMsgViewHolder(parent)
            MessageViewTypeEnum.IncVoice.ordinal -> createIncVoiceMsgViewHolder(parent)
            MessageViewTypeEnum.OutVoice.ordinal -> createOutVoiceMsgViewHolder(parent)
            MessageViewTypeEnum.IncImage.ordinal -> createIncImageMsgViewHolder(parent)
            MessageViewTypeEnum.OutImage.ordinal -> createOutImageMsgViewHolder(parent)
            MessageViewTypeEnum.IncVideo.ordinal -> createIncVideoMsgViewHolder(parent)
            MessageViewTypeEnum.OutVideo.ordinal -> createOutVideoMsgViewHolder(parent)
            MessageViewTypeEnum.IncFile.ordinal -> createIncFileMsgViewHolder(parent)
            MessageViewTypeEnum.OutFile.ordinal -> createOutFileMsgViewHolder(parent)
            MessageViewTypeEnum.IncFiles.ordinal -> createIncFilesMsgViewHolder(parent)
            MessageViewTypeEnum.OutFiles.ordinal -> createOutFilesMsgViewHolder(parent)
            MessageViewTypeEnum.IncPoll.ordinal -> createIncPollMsgViewHolder(parent)
            MessageViewTypeEnum.OutPoll.ordinal -> createOutPollMsgViewHolder(parent)
            MessageViewTypeEnum.IncDeleted.ordinal -> createIncDeletedMsgViewHolder(parent)
            MessageViewTypeEnum.OutDeleted.ordinal -> createOutDeletedMsgViewHolder(parent)
            MessageViewTypeEnum.DateSeparator.ordinal -> createDateSeparatorViewHolder(parent)
            MessageViewTypeEnum.UnreadMessagesSeparator.ordinal -> createUnreadMessagesViewHolder(parent)
            MessageViewTypeEnum.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            MessageViewTypeEnum.IncUnsupported.ordinal -> createIncUnsupportedMsgViewHolder(parent)
            MessageViewTypeEnum.OutUnsupported.ordinal -> createOutUnsupportedMsgViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createIncTextMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return IncTextMessageViewHolder(
            SceytItemIncTextMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners, displayedListener)
    }

    open fun createOutTextMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutTextMessageViewHolder(
            SceytItemOutTextMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners)
    }

    open fun createIncLinkMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return IncLinkMessageViewHolder(
            SceytItemIncLinkMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners,
            displayedListener, needMediaDataCallback)
    }

    open fun createOutLinkMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutLinkMessageViewHolder(
            SceytItemOutLinkMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners, needMediaDataCallback)
    }

    open fun createIncVoiceMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return IncVoiceMessageViewHolder(
            SceytItemIncVoiceMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners, displayedListener,
            needMediaDataCallback, voicePlayPauseListener
        )
    }

    open fun createOutVoiceMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutVoiceMessageViewHolder(
            SceytItemOutVoiceMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners,
            needMediaDataCallback, voicePlayPauseListener)
    }

    open fun createIncImageMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return IncImageMessageViewHolder(
            SceytItemIncImageMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners,
            displayedListener, needMediaDataCallback)
    }

    open fun createOutImageMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutImageMessageViewHolder(
            SceytItemOutImageMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners, needMediaDataCallback)
    }

    open fun createIncVideoMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return IncVideoMessageViewHolder(
            SceytItemIncVideoMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners,
            displayedListener, needMediaDataCallback)
    }

    open fun createOutVideoMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutVideoMessageViewHolder(
            SceytItemOutVideoMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners, needMediaDataCallback)
    }

    open fun createIncFileMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return IncFileMessageViewHolder(
            SceytItemIncFileMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners,
            displayedListener, needMediaDataCallback)
    }

    open fun createOutFileMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutFileMessageViewHolder(
            SceytItemOutFileMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners, needMediaDataCallback)
    }

    open fun createIncFilesMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return IncAttachmentsMessageViewHolder(
            SceytItemIncAttachmentsMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, viewPoolFiles, messageItemStyle.messageItemStyle,
            clickListeners, displayedListener, needMediaDataCallback)
    }

    open fun createOutFilesMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutAttachmentsMessageViewHolder(
            SceytItemOutAttachmentsMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, viewPoolFiles, messageItemStyle.messageItemStyle,
            clickListeners, needMediaDataCallback)
    }

    open fun createIncDeletedMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return IncDeletedMessageViewHolder(
            SceytItemIncDeletedMessageBinding.inflate(layoutInflater, parent, false),
            messageItemStyle.messageItemStyle, displayedListener, clickListeners)
    }

    open fun createOutDeletedMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutDeletedMessageViewHolder(
            SceytItemOutDeletedMessageBinding.inflate(layoutInflater, parent, false),
            messageItemStyle.messageItemStyle)
    }

    open fun createIncPollMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return IncPollMessageViewHolder(
            binding = SceytItemIncPollMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions = viewPoolReactions,
            style = messageItemStyle.messageItemStyle,
            messageListeners = clickListeners,
            displayedListener = displayedListener
        )
    }

    open fun createOutPollMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutPollMessageViewHolder(
            binding = SceytItemOutPollMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions = viewPoolReactions,
            style = messageItemStyle.messageItemStyle,
            messageListeners = clickListeners
        )
    }

    open fun createDateSeparatorViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return DateSeparatorViewHolder(
            SceytItemMessageDateSeparatorBinding.inflate(layoutInflater, parent, false),
            messageItemStyle
        )
    }

    open fun createUnreadMessagesViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return UnreadMessagesSeparatorViewHolder(
            SceytItemUnreadMessagesSeparatorBinding.inflate(layoutInflater, parent, false),
            messageItemStyle
        )
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return LoadingMoreMessagesViewHolder(
            SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false),
            messageItemStyle.messageItemStyle
        )
    }

    open fun createIncUnsupportedMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return IncUnsupportedMessageViewHolder(
            SceytItemIncUnsupportedMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners, displayedListener )
    }

    open fun createOutUnsupportedMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutUnsupportedMessageViewHolder(
            SceytItemOutUnsupportedMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messageItemStyle.messageItemStyle, clickListeners)
    }


    open fun getItemViewType(item: MessageListItem): Int {
        return when (item) {
            is MessageListItem.MessageItem -> getMessageType(item.message)
            is MessageListItem.DateSeparatorItem -> MessageViewTypeEnum.DateSeparator.ordinal
            is MessageListItem.UnreadMessagesSeparatorItem -> MessageViewTypeEnum.UnreadMessagesSeparator.ordinal
            is MessageListItem.LoadingPrevItem -> MessageViewTypeEnum.Loading.ordinal
            is MessageListItem.LoadingNextItem -> MessageViewTypeEnum.Loading.ordinal
        }
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
            MessageTypeEnum.Poll ->
                pick(inc, MessageViewTypeEnum.IncPoll, MessageViewTypeEnum.OutPoll).ordinal

            MessageTypeEnum.Text,
            MessageTypeEnum.Media,
            MessageTypeEnum.File,
            MessageTypeEnum.Link ->
                resolveContentViewType(inc, attachments).ordinal

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

    fun setMessageDisplayedListener(listener: (MessageListItem) -> Unit) {
        displayedListener = listener
    }

    fun setVoicePlayPauseListener(listener: (FileListItem, SceytMessage, playing: Boolean) -> Unit) {
        voicePlayPauseListener = listener
    }

    fun setNeedMediaDataCallback(callback: (NeedMediaInfoData) -> Unit) {
        needMediaDataCallback = callback
    }

    protected fun getNeedMediaDataCallback() = needMediaDataCallback

    protected fun getClickListeners() = clickListeners as MessageClickListeners.ClickListeners

    protected fun getDisplayedListener() = displayedListener

    protected fun getMessagesListViewStyle() = messageItemStyle

    enum class MessageViewTypeEnum {
        IncText,
        OutText,
        IncLink,
        OutLink,
        IncDeleted,
        OutDeleted,
        IncVoice,
        OutVoice,
        IncImage,
        OutImage,
        IncVideo,
        OutVideo,
        IncFile,
        OutFile,
        IncFiles,
        OutFiles,
        IncPoll,
        OutPoll,
        DateSeparator,
        UnreadMessagesSeparator,
        Loading,
        IncUnsupported,
        OutUnsupported
    }
}