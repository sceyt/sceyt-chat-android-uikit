package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners

import android.widget.TextView
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.customviews.SceytAvatarView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView

open class HeaderUIElementsListenerImpl(view: ConversationHeaderView) : HeaderUIElementsListener.ElementsListeners {
    private var defaultListeners: HeaderUIElementsListener.ElementsListeners = view
    private var titleListener: HeaderUIElementsListener.TitleListener? = null
    private var subTitleListener: HeaderUIElementsListener.SubTitleListener? = null
    private var avatarListener: HeaderUIElementsListener.AvatarListener? = null

    override fun onTitle(titleTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean) {
        defaultListeners.onTitle(titleTextView, channel, replyMessage, replyInThread)
        titleListener?.onTitle(titleTextView, channel, replyMessage, replyInThread)
    }

    override fun onSubTitle(subjectTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean) {
        defaultListeners.onSubTitle(subjectTextView, channel, replyMessage, replyInThread)
        subTitleListener?.onSubTitle(subjectTextView, channel, replyMessage, replyInThread)
    }

    override fun onAvatar(avatar: SceytAvatarView, channel: SceytChannel, replyInThread: Boolean) {
        defaultListeners.onAvatar(avatar, channel, replyInThread)
        avatarListener?.onAvatar(avatar, channel, replyInThread)
    }

    fun setListener(listener: HeaderUIElementsListener) {
        when (listener) {
            is HeaderUIElementsListener.ElementsListeners -> {
                titleListener = listener
                subTitleListener = listener
                avatarListener = listener
            }
            is HeaderUIElementsListener.TitleListener -> {
                titleListener = listener
            }
            is HeaderUIElementsListener.SubTitleListener -> {
                subTitleListener = listener
            }
            is HeaderUIElementsListener.AvatarListener -> {
                avatarListener = listener
            }
        }
    }
}