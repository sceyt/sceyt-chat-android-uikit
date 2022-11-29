package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners

import android.widget.TextView
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.customviews.SceytAvatarView

sealed interface HeaderUIElementsListener {

    fun interface TitleListener : HeaderUIElementsListener {
        fun onTitle(titleTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean)
    }

    fun interface SubjectListener : HeaderUIElementsListener {
        fun onSubject(subjectTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean)
    }

    fun interface AvatarListener : HeaderUIElementsListener {
        fun onAvatar(avatar: SceytAvatarView, channel: SceytChannel, replyInThread: Boolean)
    }

    /** Use this if you want to implement all callbacks */
    interface ElementsListeners : TitleListener, SubjectListener, AvatarListener
}