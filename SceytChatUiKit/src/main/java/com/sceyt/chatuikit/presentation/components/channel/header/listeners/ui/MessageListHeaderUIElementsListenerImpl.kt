package com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui

import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.styles.common.MenuStyle

open class MessageListHeaderUIElementsListenerImpl : MessageListHeaderUIElementsListener.ElementsListeners {
    @Suppress("unused")
    constructor()

    internal constructor(view: MessagesListHeaderView) {
        defaultListeners = view
    }

    private var defaultListeners: MessageListHeaderUIElementsListener.ElementsListeners? = null
    private var titleListener: MessageListHeaderUIElementsListener.TitleListener? = null
    private var subTitleListener: MessageListHeaderUIElementsListener.SubTitleListener? = null
    private var avatarListener: MessageListHeaderUIElementsListener.AvatarListener? = null
    private var actionMenuListener: MessageListHeaderUIElementsListener.ActionsMenuListener? = null
    private var toolbarActionsVisibilityListener: MessageListHeaderUIElementsListener.ToolbarActionsVisibilityListener? = null
    private var showSearchMessageListener: MessageListHeaderUIElementsListener.ShowSearchMessage? = null

    override fun onTitle(titleTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean) {
        defaultListeners?.onTitle(titleTextView, channel, replyMessage, replyInThread)
        titleListener?.onTitle(titleTextView, channel, replyMessage, replyInThread)
    }

    override fun onSubTitle(subjectTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean) {
        defaultListeners?.onSubTitle(subjectTextView, channel, replyMessage, replyInThread)
        subTitleListener?.onSubTitle(subjectTextView, channel, replyMessage, replyInThread)
    }

    override fun onAvatar(avatar: AvatarView, channel: SceytChannel, replyInThread: Boolean) {
        defaultListeners?.onAvatar(avatar, channel, replyInThread)
        avatarListener?.onAvatar(avatar, channel, replyInThread)
    }

    override fun onShowMessageActionsMenu(
            vararg messages: SceytMessage,
            menuStyle: MenuStyle,
            listener: ((MenuItem, actionFinish: () -> Unit) -> Unit)?,
    ) {
        defaultListeners?.onShowMessageActionsMenu(*messages, menuStyle = menuStyle, listener = listener)
        actionMenuListener?.onShowMessageActionsMenu(*messages, menuStyle = menuStyle, listener = listener)
    }

    override fun onHideMessageActionsMenu() {
        defaultListeners?.onHideMessageActionsMenu()
        actionMenuListener?.onHideMessageActionsMenu()
    }

    override fun onInitToolbarActionsMenu(vararg messages: SceytMessage, menu: Menu) {
        defaultListeners?.onInitToolbarActionsMenu(*messages, menu = menu)
        toolbarActionsVisibilityListener?.onInitToolbarActionsMenu(*messages, menu = menu)
    }

    override fun showSearchMessagesBar(event: MessageCommandEvent.SearchMessages) {
        defaultListeners?.showSearchMessagesBar(event)
        showSearchMessageListener?.showSearchMessagesBar(event)
    }

    fun setListener(listener: MessageListHeaderUIElementsListener) {
        when (listener) {
            is MessageListHeaderUIElementsListener.ElementsListeners -> {
                titleListener = listener
                subTitleListener = listener
                avatarListener = listener
                actionMenuListener = listener
                toolbarActionsVisibilityListener = listener
                showSearchMessageListener = listener
            }

            is MessageListHeaderUIElementsListener.TitleListener -> {
                titleListener = listener
            }

            is MessageListHeaderUIElementsListener.SubTitleListener -> {
                subTitleListener = listener
            }

            is MessageListHeaderUIElementsListener.AvatarListener -> {
                avatarListener = listener
            }

            is MessageListHeaderUIElementsListener.ActionsMenuListener -> {
                actionMenuListener = listener
            }

            is MessageListHeaderUIElementsListener.ToolbarActionsVisibilityListener -> {
                toolbarActionsVisibilityListener = listener
            }

            is MessageListHeaderUIElementsListener.ShowSearchMessage -> {
                showSearchMessageListener = listener
            }
        }
    }

    internal fun withDefaultListeners(
            listener: MessageListHeaderUIElementsListener.ElementsListeners
    ): MessageListHeaderUIElementsListenerImpl {
        defaultListeners = listener
        return this
    }
}