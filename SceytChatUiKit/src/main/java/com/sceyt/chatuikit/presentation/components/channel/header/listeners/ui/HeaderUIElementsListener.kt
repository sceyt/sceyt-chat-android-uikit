package com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui

import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.styles.common.MenuStyle

sealed interface HeaderUIElementsListener {

    fun interface TitleListener : HeaderUIElementsListener {
        fun onTitle(titleTextView: TextView,
                    channel: SceytChannel,
                    replyMessage: SceytMessage?,
                    replyInThread: Boolean)
    }

    fun interface SubTitleListener : HeaderUIElementsListener {
        fun onSubTitle(subjectTextView: TextView,
                       channel: SceytChannel,
                       replyMessage: SceytMessage?,
                       replyInThread: Boolean)
    }

    fun interface AvatarListener : HeaderUIElementsListener {
        fun onAvatar(avatar: AvatarView, channel: SceytChannel, replyInThread: Boolean)
    }

    interface ActionsMenuListener : HeaderUIElementsListener {
        fun onShowMessageActionsMenu(vararg messages: SceytMessage,
                                     menuStyle: MenuStyle,
                                     listener: ((MenuItem, actionFinish: () -> Unit) -> Unit)?)

        fun onHideMessageActionsMenu()
    }

    fun interface ToolbarActionsVisibilityListener : HeaderUIElementsListener {
        fun onInitToolbarActionsMenu(vararg messages: SceytMessage, menu: Menu)
    }

    fun interface ShowSearchMessage : HeaderUIElementsListener {
        fun showSearchMessagesBar(event: MessageCommandEvent.SearchMessages)
    }


    /** Use this if you want to implement all callbacks */
    interface ElementsListeners : TitleListener, SubTitleListener, AvatarListener,
            ActionsMenuListener, ToolbarActionsVisibilityListener, ShowSearchMessage
}