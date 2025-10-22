package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemIncAttachmentsMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncDeletedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncFileMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncImageMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncLinkMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncTextMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncVideoMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncPollMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncVoiceMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.chatuikit.databinding.SceytItemOutAttachmentsMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutDeletedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutPollMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutFileMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutImageMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutLinkMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutTextMessageBinding
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
    protected val viewPoolPollOptions = RecyclerView.RecycledViewPool()
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
            viewPoolPollOptions = viewPoolPollOptions,
            style = messageItemStyle.messageItemStyle,
            messageListeners = clickListeners,
            displayedListener = displayedListener
        )
    }

    open fun createOutPollMsgViewHolder(parent: ViewGroup): BaseMessageViewHolder {
        return OutPollMessageViewHolder(
            binding = SceytItemOutPollMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions = viewPoolReactions,
            viewPoolPollOptions = viewPoolPollOptions,
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

    open fun getItemViewType(item: MessageListItem): Int {
        return when (item) {
            is MessageListItem.MessageItem -> getMessageType(item.message)
            is MessageListItem.DateSeparatorItem -> MessageViewTypeEnum.DateSeparator.ordinal
            is MessageListItem.UnreadMessagesSeparatorItem -> MessageViewTypeEnum.UnreadMessagesSeparator.ordinal
            is MessageListItem.LoadingPrevItem -> MessageViewTypeEnum.Loading.ordinal
            is MessageListItem.LoadingNextItem -> MessageViewTypeEnum.Loading.ordinal
        }
    }

    open fun getMessageType(message: SceytMessage): Int {
        val inc = message.incoming
        val attachments = message.attachments
        val type = when {
            message.state == MessageState.Deleted -> if (inc) MessageViewTypeEnum.IncDeleted else MessageViewTypeEnum.OutDeleted
            message.type == MessageTypeEnum.Poll.value -> if (inc) MessageViewTypeEnum.IncPoll else MessageViewTypeEnum.OutPoll
            !attachments.isNullOrEmpty() -> {
                val (links, others) = attachments.partition { it.type == AttachmentTypeEnum.Link.value }
                //Check maybe all attachments are links
                if (links.size == attachments.size)
                    return if (inc) MessageViewTypeEnum.IncLink.ordinal else MessageViewTypeEnum.OutLink.ordinal

                val attachment = others.firstOrNull()
                when (attachment?.type) {
                    AttachmentTypeEnum.Image.value -> if (inc) MessageViewTypeEnum.IncImage else MessageViewTypeEnum.OutImage
                    AttachmentTypeEnum.Video.value -> if (inc) MessageViewTypeEnum.IncVideo else MessageViewTypeEnum.OutVideo
                    AttachmentTypeEnum.File.value -> if (inc) MessageViewTypeEnum.IncFile else MessageViewTypeEnum.OutFile
                    AttachmentTypeEnum.Voice.value -> if (inc) MessageViewTypeEnum.IncVoice else MessageViewTypeEnum.OutVoice
                    else -> if (inc) MessageViewTypeEnum.IncFiles else MessageViewTypeEnum.OutFiles
                }
            }

            else -> if (inc) MessageViewTypeEnum.IncText else MessageViewTypeEnum.OutText
        }
        return type.ordinal
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
        Loading
    }
}