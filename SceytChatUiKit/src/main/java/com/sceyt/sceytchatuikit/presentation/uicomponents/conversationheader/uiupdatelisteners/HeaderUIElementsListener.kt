package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners

import android.view.Menu
import android.view.MenuItem
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.MenuRes
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.customviews.SceytAvatarView

sealed interface HeaderUIElementsListener {

    fun interface TitleListener : HeaderUIElementsListener {
        fun onTitle(titleTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean)
    }

    fun interface SubTitleListener : HeaderUIElementsListener {
        fun onSubTitle(subjectTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean)
    }

    fun interface AvatarListener : HeaderUIElementsListener {
        fun onAvatar(avatar: SceytAvatarView, channel: SceytChannel, replyInThread: Boolean)
    }

    fun interface ActionsMenuListener : HeaderUIElementsListener {
        fun onShowMessageActionsMenu(message: SceytMessage, @MenuRes menuResId: Int, reactionsPopupWindow: PopupWindow?, listener: ((MenuItem) -> Unit)?): Menu?
    }

    /** Use this if you want to implement all callbacks */
    interface ElementsListeners : TitleListener, SubTitleListener, AvatarListener, ActionsMenuListener
}