package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners

import android.widget.TextView
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.customviews.SceytAvatarView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView

open class HeaderUIElementsListenerImpl(view: ConversationHeaderView) : HeaderUIElementsListener.ElementsListeners {
    private var defaultListeners: HeaderUIElementsListener.ElementsListeners = view
    private var titleListener: HeaderUIElementsListener.TitleListener? = null
    private var subjectListener: HeaderUIElementsListener.SubjectListener? = null
    private var avatarListener: HeaderUIElementsListener.AvatarListener? = null

    override fun onTitle(titleTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean) {
        defaultListeners.onTitle(titleTextView, channel, replyMessage, replyInThread)
        titleListener?.onTitle(titleTextView, channel, replyMessage, replyInThread)
    }

    override fun onSubject(subjectTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean) {
        defaultListeners.onSubject(subjectTextView, channel, replyMessage, replyInThread)
        subjectListener?.onSubject(subjectTextView, channel, replyMessage, replyInThread)
    }

    override fun onAvatar(avatar: SceytAvatarView, channel: SceytChannel, replyInThread: Boolean) {
        defaultListeners.onAvatar(avatar, channel, replyInThread)
        avatarListener?.onAvatar(avatar, channel, replyInThread)
    }

    fun setListener(listener: HeaderUIElementsListener) {
        when (listener) {
            is HeaderUIElementsListener.ElementsListeners -> {
                titleListener = listener
                subjectListener = listener
                avatarListener = listener
            }
            is HeaderUIElementsListener.TitleListener -> {
                titleListener = listener
            }
            is HeaderUIElementsListener.SubjectListener -> {
                subjectListener = listener
            }
            is HeaderUIElementsListener.AvatarListener -> {
                avatarListener = listener
            }
        }
    }
}