package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemIncAttachmentsMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncDeletedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncFileMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncImageMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncLinkMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncTextMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncVideoMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemIncVoiceMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.chatuikit.databinding.SceytItemOutAttachmentsMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutDeletedMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutFileMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutImageMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutLinkMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutTextMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutVideoMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemOutVoiceMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemUnreadMessagesSeparatorBinding
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.DateSeparatorViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.IncAttachmentsMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.IncDeletedMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.IncFileMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.IncImageMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.IncLinkMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.IncTextMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.IncVideoMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.IncVoiceMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.LoadingMoreMessagesViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutAttachmentsMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutDeletedMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutFileMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutImageMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutLinkMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutTextMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutVideoMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutVoiceMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.UnreadMessagesSeparatorViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle
import com.sceyt.chatuikit.shared.helpers.LinkPreviewHelper

open class MessageViewHolderFactory(context: Context) {

    protected val linkPreview: LinkPreviewHelper = LinkPreviewHelper(context)
    protected val viewPoolReactions = RecyclerView.RecycledViewPool()
    protected val viewPoolFiles = RecyclerView.RecycledViewPool()
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var clickListeners = MessageClickListenersImpl()
    private var displayedListener: ((MessageListItem) -> Unit)? = null
    private var voicePlayPauseListener: ((FileListItem, playing: Boolean) -> Unit)? = null
    private var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder
    private var needMediaDataCallback: (NeedMediaInfoData) -> Unit = {}
    internal lateinit var messagesListViewStyle: MessagesListViewStyle

    internal fun setStyle(style: MessagesListViewStyle) {
        this.messagesListViewStyle = style
    }

    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseMsgViewHolder {
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
            MessageViewTypeEnum.IncDeleted.ordinal -> createIncDeletedMsgViewHolder(parent)
            MessageViewTypeEnum.OutDeleted.ordinal -> createOutDeletedMsgViewHolder(parent)
            MessageViewTypeEnum.DateSeparator.ordinal -> createDateSeparatorViewHolder(parent)
            MessageViewTypeEnum.UnreadMessagesSeparator.ordinal -> createUnreadMessagesViewHolder(parent)
            MessageViewTypeEnum.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createIncTextMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncTextMsgViewHolder(
            SceytItemIncTextMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messagesListViewStyle, clickListeners, displayedListener, userNameBuilder)
    }

    open fun createOutTextMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutTextMsgViewHolder(
            SceytItemOutTextMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messagesListViewStyle, clickListeners, userNameBuilder)
    }

    open fun createIncLinkMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncLinkMsgViewHolder(
            SceytItemIncLinkMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, linkPreview, messagesListViewStyle, clickListeners, displayedListener, userNameBuilder)
    }

    open fun createOutLinkMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutLinkMsgViewHolder(
            SceytItemOutLinkMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, linkPreview, messagesListViewStyle, clickListeners, userNameBuilder)
    }

    open fun createIncVoiceMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncVoiceMsgViewHolder(
            SceytItemIncVoiceMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messagesListViewStyle, clickListeners, displayedListener, userNameBuilder,
            needMediaDataCallback, voicePlayPauseListener
        )
    }

    open fun createOutVoiceMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutVoiceMsgViewHolder(
            SceytItemOutVoiceMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messagesListViewStyle, clickListeners, userNameBuilder,
            needMediaDataCallback, voicePlayPauseListener)
    }

    open fun createIncImageMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncImageMsgViewHolder(
            SceytItemIncImageMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messagesListViewStyle, clickListeners, displayedListener, userNameBuilder, needMediaDataCallback)
    }

    open fun createOutImageMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutImageMsgViewHolder(
            SceytItemOutImageMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messagesListViewStyle, clickListeners, userNameBuilder, needMediaDataCallback)
    }

    open fun createIncVideoMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncVideoMsgViewHolder(
            SceytItemIncVideoMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messagesListViewStyle, clickListeners, displayedListener, userNameBuilder, needMediaDataCallback)
    }

    open fun createOutVideoMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutVideoMsgViewHolder(
            SceytItemOutVideoMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messagesListViewStyle, clickListeners, userNameBuilder, needMediaDataCallback)
    }

    open fun createIncFileMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncFileMsgViewHolder(
            SceytItemIncFileMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messagesListViewStyle, clickListeners, displayedListener, userNameBuilder, needMediaDataCallback)
    }

    open fun createOutFileMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutFileMsgViewHolder(
            SceytItemOutFileMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, messagesListViewStyle, clickListeners, userNameBuilder, needMediaDataCallback)
    }

    open fun createIncFilesMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncAttachmentsMsgViewHolder(
            SceytItemIncAttachmentsMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, viewPoolFiles, messagesListViewStyle, clickListeners, displayedListener, userNameBuilder, needMediaDataCallback)
    }

    open fun createOutFilesMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutAttachmentsMsgViewHolder(
            SceytItemOutAttachmentsMessageBinding.inflate(layoutInflater, parent, false),
            viewPoolReactions, viewPoolFiles, messagesListViewStyle, clickListeners, userNameBuilder, needMediaDataCallback)
    }

    open fun createIncDeletedMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return IncDeletedMsgViewHolder(
            SceytItemIncDeletedMessageBinding.inflate(layoutInflater, parent, false),
            messagesListViewStyle, userNameBuilder, displayedListener, clickListeners)
    }

    open fun createOutDeletedMsgViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return OutDeletedMsgViewHolder(
            SceytItemOutDeletedMessageBinding.inflate(layoutInflater, parent, false),
            messagesListViewStyle)
    }

    open fun createDateSeparatorViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return DateSeparatorViewHolder(
            SceytItemMessageDateSeparatorBinding.inflate(layoutInflater, parent, false),
            messagesListViewStyle
        )
    }

    open fun createUnreadMessagesViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return UnreadMessagesSeparatorViewHolder(
            SceytItemUnreadMessagesSeparatorBinding.inflate(layoutInflater, parent, false),
            messagesListViewStyle
        )
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseMsgViewHolder {
        return LoadingMoreMessagesViewHolder(
            SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false),
            messagesListViewStyle
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
            !attachments.isNullOrEmpty() -> {
                val (links, others) = attachments.partition { it.type == AttachmentTypeEnum.Link.value() }
                //Check maybe all attachments are links
                if (links.size == attachments.size)
                    return if (inc) MessageViewTypeEnum.IncLink.ordinal else MessageViewTypeEnum.OutLink.ordinal

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

    fun setMessageListener(listener: MessageClickListeners) {
        clickListeners.setListener(listener)
    }

    fun setMessageDisplayedListener(listener: (MessageListItem) -> Unit) {
        displayedListener = listener
    }

    fun setVoicePlayPauseListener(listener: (FileListItem, playing: Boolean) -> Unit) {
        voicePlayPauseListener = listener
    }

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
    }

    fun setNeedMediaDataCallback(callback: (NeedMediaInfoData) -> Unit) {
        needMediaDataCallback = callback
    }

    protected fun getNeedMediaDataCallback() = needMediaDataCallback

    protected fun getClickListeners() = clickListeners as MessageClickListeners.ClickListeners

    protected fun getDisplayedListener() = displayedListener

    protected fun getUserNameBuilder() = userNameBuilder

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
        DateSeparator,
        UnreadMessagesSeparator,
        Loading
    }
}