package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners

import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.customviews.SceytAvatarView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView

open class HeaderUIElementsListenerImpl(view: ConversationHeaderView) : HeaderUIElementsListener.ElementsListeners {
    private var defaultListeners: HeaderUIElementsListener.ElementsListeners = view
    private var titleListener: HeaderUIElementsListener.TitleListener? = null
    private var subTitleListener: HeaderUIElementsListener.SubTitleListener? = null
    private var avatarListener: HeaderUIElementsListener.AvatarListener? = null
    private var actionMenuListener: HeaderUIElementsListener.ActionsMenuListener? = null
    private var toolbarActionsVisibilityListener: HeaderUIElementsListener.ToolbarActionsVisibilityListener? = null
    private var showSearchMessageListener: HeaderUIElementsListener.ShowSearchMessage? = null

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

    override fun onShowMessageActionsMenu(vararg messages: SceytMessage, menuResId: Int,
                                          listener: ((MenuItem, actionFinish: () -> Unit) -> Unit)?): Menu? {
        val menu = defaultListeners.onShowMessageActionsMenu(*messages, menuResId = menuResId, listener = listener)
        return actionMenuListener?.onShowMessageActionsMenu(*messages, menuResId = menuResId, listener = listener)
                ?: menu
    }

    override fun onHideMessageActionsMenu() {
        defaultListeners.onHideMessageActionsMenu()
        actionMenuListener?.onHideMessageActionsMenu()
    }

    override fun onInitToolbarActionsMenu(vararg messages: SceytMessage, menu: Menu) {
        defaultListeners.onInitToolbarActionsMenu(*messages, menu = menu)
        toolbarActionsVisibilityListener?.onInitToolbarActionsMenu(*messages, menu = menu)
    }

    override fun showSearchMessagesBar(event: MessageCommandEvent.SearchMessages) {
        defaultListeners.showSearchMessagesBar(event)
        showSearchMessageListener?.showSearchMessagesBar(event)
    }

    fun setListener(listener: HeaderUIElementsListener) {
        when (listener) {
            is HeaderUIElementsListener.ElementsListeners -> {
                titleListener = listener
                subTitleListener = listener
                avatarListener = listener
                actionMenuListener = listener
                toolbarActionsVisibilityListener = listener
                showSearchMessageListener = listener
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

            is HeaderUIElementsListener.ActionsMenuListener -> {
                actionMenuListener = listener
            }

            is HeaderUIElementsListener.ToolbarActionsVisibilityListener -> {
                toolbarActionsVisibilityListener = listener
            }

            is HeaderUIElementsListener.ShowSearchMessage -> {
                showSearchMessageListener = listener
            }
        }
    }
}