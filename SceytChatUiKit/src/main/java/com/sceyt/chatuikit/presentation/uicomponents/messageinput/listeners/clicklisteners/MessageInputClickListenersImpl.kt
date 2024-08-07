package com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners

import android.view.View
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.CancelLinkPreviewClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.CancelReplyMessageViewClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.ClearChatClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.ClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.JoinClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.RemoveAttachmentClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.ScrollToNextMessageClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.ScrollToPreviousMessageClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.SelectedUserToMentionClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.SendAttachmentClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.SendMsgClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.VoiceClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.VoiceLongClickListener

open class MessageInputClickListenersImpl(view: MessageInputView) : ClickListeners {
    private var defaultListeners: ClickListeners = view
    private var sendMsgClickListener: SendMsgClickListener? = null
    private var addAttachmentClickListener: SendAttachmentClickListener? = null
    private var voiceClickListener: VoiceClickListener? = null
    private var voiceLongClickListener: VoiceLongClickListener? = null
    private var cancelReplyMessageViewClickListener: CancelReplyMessageViewClickListener? = null
    private var cancelLinkPreviewClickListener: CancelLinkPreviewClickListener? = null
    private var removeAttachmentClickListener: RemoveAttachmentClickListener? = null
    private var joinClickListener: JoinClickListener? = null
    private var clearChatClickListener: ClearChatClickListener? = null
    private var scrollToNextMessageClickListener: ScrollToNextMessageClickListener? = null
    private var scrollToPreviousMessageClickListener: ScrollToPreviousMessageClickListener? = null
    private var onSelectUserToMentionClickListener: SelectedUserToMentionClickListener? = null

    override fun onSendMsgClick(view: View) {
        defaultListeners.onSendMsgClick(view)
        sendMsgClickListener?.onSendMsgClick(view)
    }

    override fun onAddAttachmentClick(view: View) {
        defaultListeners.onAddAttachmentClick(view)
        addAttachmentClickListener?.onAddAttachmentClick(view)
    }

    override fun onVoiceClick(view: View) {
        defaultListeners.onVoiceClick(view)
        voiceClickListener?.onVoiceClick(view)
    }

    override fun onVoiceLongClick(view: View) {
        defaultListeners.onVoiceLongClick(view)
        voiceLongClickListener?.onVoiceLongClick(view)
    }

    override fun onCancelReplyMessageViewClick(view: View) {
        defaultListeners.onCancelReplyMessageViewClick(view)
        cancelReplyMessageViewClickListener?.onCancelReplyMessageViewClick(view)
    }

    override fun onCancelLinkPreviewClick(view: View) {
        defaultListeners.onCancelLinkPreviewClick(view)
        cancelLinkPreviewClickListener?.onCancelLinkPreviewClick(view)
    }

    override fun onRemoveAttachmentClick(item: AttachmentItem) {
        defaultListeners.onRemoveAttachmentClick(item)
        removeAttachmentClickListener?.onRemoveAttachmentClick(item)
    }

    override fun onJoinClick() {
        defaultListeners.onJoinClick()
        joinClickListener?.onJoinClick()
    }

    override fun onClearChatClick() {
        defaultListeners.onClearChatClick()
        clearChatClickListener?.onClearChatClick()
    }

    override fun onScrollToNextMessageClick() {
        defaultListeners.onScrollToNextMessageClick()
        scrollToNextMessageClickListener?.onScrollToNextMessageClick()
    }

    override fun onScrollToPreviousMessageClick() {
        defaultListeners.onScrollToPreviousMessageClick()
        scrollToPreviousMessageClickListener?.onScrollToPreviousMessageClick()
    }

    override fun onSelectedUserToMentionClick(member: SceytMember) {
        defaultListeners.onSelectedUserToMentionClick(member)
        onSelectUserToMentionClickListener?.onSelectedUserToMentionClick(member)
    }

    fun setListener(listener: MessageInputClickListeners) {
        when (listener) {
            is ClickListeners -> {
                sendMsgClickListener = listener
                addAttachmentClickListener = listener
                voiceClickListener = listener
                voiceLongClickListener = listener
                cancelReplyMessageViewClickListener = listener
                removeAttachmentClickListener = listener
                joinClickListener = listener
                clearChatClickListener = listener
                scrollToNextMessageClickListener = listener
                scrollToPreviousMessageClickListener = listener
                onSelectUserToMentionClickListener = listener
            }

            is SendMsgClickListener -> {
                sendMsgClickListener = listener
            }

            is SendAttachmentClickListener -> {
                addAttachmentClickListener = listener
            }

            is VoiceClickListener -> {
                voiceClickListener = listener
            }

            is VoiceLongClickListener -> {
                voiceLongClickListener = listener
            }

            is CancelReplyMessageViewClickListener -> {
                cancelReplyMessageViewClickListener = listener
            }

            is CancelLinkPreviewClickListener -> {
                cancelLinkPreviewClickListener = listener
            }

            is RemoveAttachmentClickListener -> {
                removeAttachmentClickListener = listener
            }

            is JoinClickListener -> {
                joinClickListener = listener
            }

            is ClearChatClickListener -> {
                clearChatClickListener = listener
            }

            is ScrollToNextMessageClickListener -> {
                scrollToNextMessageClickListener = listener
            }

            is ScrollToPreviousMessageClickListener -> {
                scrollToPreviousMessageClickListener = listener
            }

            is SelectedUserToMentionClickListener -> {
                onSelectUserToMentionClickListener = listener
            }
        }
    }
}