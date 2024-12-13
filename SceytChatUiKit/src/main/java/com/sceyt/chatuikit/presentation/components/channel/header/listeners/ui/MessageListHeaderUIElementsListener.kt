package com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui

import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.styles.common.MenuStyle

sealed interface MessageListHeaderUIElementsListener {

    fun interface TitleListener : MessageListHeaderUIElementsListener {
        fun onTitle(titleTextView: TextView,
                    channel: SceytChannel,
                    replyMessage: SceytMessage?,
                    replyInThread: Boolean)
    }

    fun interface SubTitleListener : MessageListHeaderUIElementsListener {
        fun onSubTitle(subjectTextView: TextView,
                       channel: SceytChannel,
                       replyMessage: SceytMessage?,
                       replyInThread: Boolean)
    }

    fun interface AvatarListener : MessageListHeaderUIElementsListener {
        fun onAvatar(avatar: AvatarView, channel: SceytChannel, replyInThread: Boolean)
    }

    interface ActionsMenuListener : MessageListHeaderUIElementsListener {
        fun onShowMessageActionsMenu(vararg messages: SceytMessage,
                                     menuStyle: MenuStyle,
                                     listener: ((MenuItem, actionFinish: () -> Unit) -> Unit)?)

        fun onHideMessageActionsMenu()
    }

    fun interface ToolbarActionsVisibilityListener : MessageListHeaderUIElementsListener {
        fun onInitToolbarActionsMenu(vararg messages: SceytMessage, menu: Menu)
    }

    fun interface ShowSearchMessage : MessageListHeaderUIElementsListener {
        fun showSearchMessagesBar(event: MessageCommandEvent.SearchMessages)
    }


    /** Use this if you want to implement all callbacks */
    interface ElementsListeners : TitleListener, SubTitleListener, AvatarListener,
            ActionsMenuListener, ToolbarActionsVisibilityListener, ShowSearchMessage
}