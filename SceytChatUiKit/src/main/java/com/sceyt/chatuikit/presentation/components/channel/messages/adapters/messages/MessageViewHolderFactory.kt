package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.databinding.SceytItemIncAttachmentsMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncDeletedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncFileMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncImageMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncLinkMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncPollMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncSelfDestructedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncSelfDestructingMessageBinding
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
import com.sceyt.chatuikit.databinding.SceytItemOutSelfDestructedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutSelfDestructingMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutTextMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutUnsupportedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutVideoMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutVoiceMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemSystemMessageBinding
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
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncSelfDestructedMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.IncSelfDestructingMessageViewHolder
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
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutSelfDestructedMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutSelfDestructingMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutTextMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutUnsupportedMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutVideoMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.OutVoiceMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.SystemMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders.UnreadMessagesSeparatorViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListenersImpl
import com.sceyt.chatuikit.presentation.extensions.getMessageType
import com.sceyt.chatuikit.presentation.extensions.isSelfDestructed
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle

open class MessageViewHolderFactory(context: Context) {
    protected val viewPoolReactions = RecyclerView.RecycledViewPool()
    protected val viewPoolFiles = RecyclerView.RecycledViewPool()
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var messageItemStyle: MessagesListViewStyle
    private var clickListeners = MessageClickListenersImpl()
    private var displayedListener: ((MessageListItem) -> Unit)? = null
    private var voicePlayPauseListener: ((FileListItem, SceytMessage, playing: Boolean) -> Unit)? =
        null
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
            MessageViewTypeEnum.IncVoice.ordinal -> createIncVoiceMsgViewHolder(parent, false)
            MessageViewTypeEnum.OutVoice.ordinal -> createOutVoiceMsgViewHolder(parent, false)
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
            MessageViewTypeEnum.IncSelfDestructingMedia.ordinal -> {
                createIncSelfDestructingMessageViewHolder(parent)
            }

            MessageViewTypeEnum.OutSelfDestructingMedia.ordinal -> {
                createOutSelfDestructingMessageViewHolder(parent)
            }

            MessageViewTypeEnum.IncSelfDestructingVoice.ordinal -> {
                createIncVoiceMsgViewHolder(parent, true)
            }

            MessageViewTypeEnum.OutSelfDestructingVoice.ordinal -> {
                createOutVoiceMsgViewHolder(parent, true)
            }

            MessageViewTypeEnum.IncSelfDestructed.ordinal -> {
                createIncSelfDestructedMessageViewHolder(parent)
            }

            MessageViewTypeEnum.OutSelfDestructed.ordinal -> {
                createOutSelfDestructedMessageViewHolder(parent)
            }

            MessageViewTypeEnum.IncUnsupported.ordinal -> createIncUnsupportedMsgViewHolder(parent)
            MessageViewTypeEnum.OutUnsupported.ordinal -> createOutUnsupportedMsgViewHolder(parent)
            MessageViewTypeEnum.IncDeleted.ordinal -> createIncDeletedMsgViewHolder(parent)
            MessageViewTypeEnum.OutDeleted.ordinal -> createOutDeletedMsgViewHolder(parent)
            MessageViewTypeEnum.DateSeparator.ordinal -> createDateSeparatorViewHolder(parent)
            MessageViewTypeEnum.UnreadMessagesSeparator.ordinal -> {
                createUnreadMessagesViewHolder(parent)
            }

            MessageViewTypeEnum.System.ordinal -> createSystemMessageViewHolder(parent)
            MessageViewTypeEnum.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createIncTextMsgViewHolder(parent: ViewGroup) = IncTextMessageViewHolder(
        binding = SceytItemIncTextMessageBinding.inflate(layoutInflater, parent, false),
        viewPool = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        displayedListener = displayedListener
    )

    open fun createOutTextMsgViewHolder(parent: ViewGroup) = OutTextMessageViewHolder(
        binding = SceytItemOutTextMessageBinding.inflate(layoutInflater, parent, false),
        viewPool = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners
    )

    open fun createIncLinkMsgViewHolder(parent: ViewGroup) = IncLinkMessageViewHolder(
        binding = SceytItemIncLinkMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        displayedListener = displayedListener
    )

    open fun createOutLinkMsgViewHolder(parent: ViewGroup) = OutLinkMessageViewHolder(
        binding = SceytItemOutLinkMessageBinding.inflate(layoutInflater, parent, false),
        viewPool = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
    )

    open fun createIncVoiceMsgViewHolder(
        parent: ViewGroup,
        isViewOnce: Boolean
    ) = IncVoiceMessageViewHolder(
        binding = SceytItemIncVoiceMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        isViewOnce = isViewOnce,
        messageListeners = clickListeners,
        displayedListener = displayedListener,
        needMediaDataCallback = needMediaDataCallback,
        voicePlayPauseListener = voicePlayPauseListener
    )

    open fun createOutVoiceMsgViewHolder(
        parent: ViewGroup,
        isViewOnce: Boolean
    ) = OutVoiceMessageViewHolder(
        binding = SceytItemOutVoiceMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        isViewOnce = isViewOnce,
        messageListeners = clickListeners,
        needMediaDataCallback = needMediaDataCallback,
        voicePlayPauseListener = voicePlayPauseListener
    )

    open fun createIncImageMsgViewHolder(parent: ViewGroup) = IncImageMessageViewHolder(
        binding = SceytItemIncImageMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        displayedListener = displayedListener,
        needMediaDataCallback = needMediaDataCallback
    )

    open fun createOutImageMsgViewHolder(parent: ViewGroup) = OutImageMessageViewHolder(
        binding = SceytItemOutImageMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        needMediaDataCallback = needMediaDataCallback
    )


    open fun createIncVideoMsgViewHolder(parent: ViewGroup) = IncVideoMessageViewHolder(
        binding = SceytItemIncVideoMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        displayedListener = displayedListener,
        needMediaDataCallback = needMediaDataCallback
    )

    open fun createOutVideoMsgViewHolder(parent: ViewGroup) = OutVideoMessageViewHolder(
        binding = SceytItemOutVideoMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        needMediaDataCallback = needMediaDataCallback
    )


    open fun createIncFileMsgViewHolder(parent: ViewGroup) = IncFileMessageViewHolder(
        binding = SceytItemIncFileMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        displayedListener = displayedListener,
        needMediaDataCallback = needMediaDataCallback
    )

    open fun createOutFileMsgViewHolder(parent: ViewGroup) = OutFileMessageViewHolder(
        binding = SceytItemOutFileMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        needMediaDataCallback = needMediaDataCallback
    )

    open fun createIncFilesMsgViewHolder(parent: ViewGroup) = IncAttachmentsMessageViewHolder(
        binding = SceytItemIncAttachmentsMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        viewPoolFiles = viewPoolFiles,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        displayedListener = displayedListener,
        needMediaDataCallback = needMediaDataCallback
    )

    open fun createOutFilesMsgViewHolder(parent: ViewGroup) = OutAttachmentsMessageViewHolder(
        binding = SceytItemOutAttachmentsMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        viewPoolFiles = viewPoolFiles,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        needMediaDataCallback = needMediaDataCallback
    )

    open fun createIncDeletedMsgViewHolder(parent: ViewGroup) = IncDeletedMessageViewHolder(
        binding = SceytItemIncDeletedMessageBinding.inflate(layoutInflater, parent, false),
        style = messageItemStyle.messageItemStyle,
        displayedListener = displayedListener,
        messageListeners = clickListeners
    )

    open fun createOutDeletedMsgViewHolder(parent: ViewGroup) = OutDeletedMessageViewHolder(
        binding = SceytItemOutDeletedMessageBinding.inflate(layoutInflater, parent, false),
        style = messageItemStyle.messageItemStyle
    )

    open fun createIncPollMsgViewHolder(parent: ViewGroup) = IncPollMessageViewHolder(
        binding = SceytItemIncPollMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        displayedListener = displayedListener
    )

    open fun createOutPollMsgViewHolder(parent: ViewGroup) = OutPollMessageViewHolder(
        binding = SceytItemOutPollMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners
    )

    open fun createDateSeparatorViewHolder(parent: ViewGroup) = DateSeparatorViewHolder(
        binding = SceytItemMessageDateSeparatorBinding.inflate(layoutInflater, parent, false),
        listStyle = messageItemStyle
    )

    open fun createUnreadMessagesViewHolder(
        parent: ViewGroup
    ) = UnreadMessagesSeparatorViewHolder(
        binding = SceytItemUnreadMessagesSeparatorBinding.inflate(layoutInflater, parent, false),
        listViewStyle = messageItemStyle
    )

    open fun createLoadingMoreViewHolder(parent: ViewGroup) = LoadingMoreMessagesViewHolder(
        binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false),
        style = messageItemStyle.messageItemStyle
    )

    open fun createIncUnsupportedMsgViewHolder(parent: ViewGroup) = IncUnsupportedMessageViewHolder(
        binding = SceytItemIncUnsupportedMessageBinding.inflate(layoutInflater, parent, false),
        viewPool = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        displayedListener = displayedListener
    )

    open fun createOutUnsupportedMsgViewHolder(parent: ViewGroup) = OutUnsupportedMessageViewHolder(
        binding = SceytItemOutUnsupportedMessageBinding.inflate(layoutInflater, parent, false),
        viewPool = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners
    )

    open fun createSystemMessageViewHolder(parent: ViewGroup) = SystemMessageViewHolder(
        binding = SceytItemSystemMessageBinding.inflate(layoutInflater, parent, false),
        style = messageItemStyle.messageItemStyle,
        displayedListener = displayedListener
    )

    open fun createOutSelfDestructingMessageViewHolder(
        parent: ViewGroup
    ) = OutSelfDestructingMessageViewHolder(
        binding = SceytItemOutSelfDestructingMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        needMediaDataCallback = needMediaDataCallback
    )

    open fun createIncSelfDestructingMessageViewHolder(
        parent: ViewGroup
    ) = IncSelfDestructingMessageViewHolder(
        binding = SceytItemIncSelfDestructingMessageBinding.inflate(layoutInflater, parent, false),
        viewPoolReactions = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        displayedListener = displayedListener,
        needMediaDataCallback = needMediaDataCallback
    )

    open fun createOutSelfDestructedMessageViewHolder(
        parent: ViewGroup
    ) = OutSelfDestructedMessageViewHolder(
        binding = SceytItemOutSelfDestructedMessageBinding.inflate(layoutInflater, parent, false),
        viewPool = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners
    )

    open fun createIncSelfDestructedMessageViewHolder(
        parent: ViewGroup
    ) = IncSelfDestructedMessageViewHolder(
        binding = SceytItemIncSelfDestructedMessageBinding.inflate(layoutInflater, parent, false),
        viewPool = viewPoolReactions,
        style = messageItemStyle.messageItemStyle,
        messageListeners = clickListeners,
        displayedListener = displayedListener
    )


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
            return pick(
                inc = inc,
                incType = MessageViewTypeEnum.IncDeleted,
                outType = MessageViewTypeEnum.OutDeleted
            ).ordinal
        }

        return when (message.getMessageType()) {
            SceytMessageType.System -> MessageViewTypeEnum.System.ordinal
            SceytMessageType.Poll -> pick(
                inc = inc,
                incType = MessageViewTypeEnum.IncPoll,
                outType = MessageViewTypeEnum.OutPoll
            ).ordinal

            SceytMessageType.Text,
            SceytMessageType.Media,
            SceytMessageType.File,
            SceytMessageType.Link -> resolveContentViewType(
                inc = inc,
                attachments = attachments
            ).ordinal

            SceytMessageType.ViewOnce -> resolveViewOnceType(inc, message).ordinal
            else -> pick(
                inc = inc,
                incType = MessageViewTypeEnum.IncUnsupported,
                outType = MessageViewTypeEnum.OutUnsupported
            ).ordinal
        }
    }

    private fun resolveViewOnceType(
        inc: Boolean,
        message: SceytMessage
    ): MessageViewTypeEnum {
        val hasOpenedMarker = message.isSelfDestructed()

        if (hasOpenedMarker) {
            return pick(
                inc = inc,
                incType = MessageViewTypeEnum.IncSelfDestructed,
                outType = MessageViewTypeEnum.OutSelfDestructed
            )
        }

        val isSelfDestructingVoiceMessage =
            message.attachments?.firstOrNull()?.type == AttachmentTypeEnum.Voice.value
        if (isSelfDestructingVoiceMessage) {
            return pick(
                inc = inc,
                incType = MessageViewTypeEnum.IncSelfDestructingVoice,
                outType = MessageViewTypeEnum.OutSelfDestructingVoice
            )
        }

        return pick(
            inc = inc,
            incType = MessageViewTypeEnum.IncSelfDestructingMedia,
            outType = MessageViewTypeEnum.OutSelfDestructingMedia
        )
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
        IncSelfDestructingMedia,
        OutSelfDestructingMedia,
        IncSelfDestructingVoice,
        OutSelfDestructingVoice,
        IncSelfDestructed,
        OutSelfDestructed,
        IncUnsupported,
        OutUnsupported,
        DateSeparator,
        UnreadMessagesSeparator,
        Loading,
        System
    }
}