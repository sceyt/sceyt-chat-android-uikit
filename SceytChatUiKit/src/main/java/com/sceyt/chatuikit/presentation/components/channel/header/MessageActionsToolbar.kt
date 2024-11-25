package com.sceyt.chatuikit.presentation.components.channel.header

import android.content.Context
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.MenuItem
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.changeAlphaWithAnim
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui.MessageListHeaderUIElementsListener.ToolbarActionsVisibilityListener
import com.sceyt.chatuikit.styles.common.MenuStyle

class MessageActionsToolbar @JvmOverloads constructor(
        context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0,
) : Toolbar(context, attributeSet, defStyleAttr) {

    private var itemClickListener: ((MenuItem) -> Unit)? = null
    private var visibilityInitializer: ToolbarActionsVisibilityListener? = null

    @Volatile
    var handledClick: Boolean = false
        private set

    private fun initMenu(menuStyle: MenuStyle, vararg messages: SceytMessage) {
        menu.clear()
        menuStyle.apply(this)
        setOnMenuItemClickListener {
            handledClick = true
            itemClickListener?.invoke(it)
            return@setOnMenuItemClickListener true
        }

        visibilityInitializer?.onInitToolbarActionsMenu(*messages, menu = menu)
    }

    fun setupMenuWithMessages(menuStyle: MenuStyle, vararg messages: SceytMessage) {
        initMenu(menuStyle, *messages)

        setTitleMargin(dpToPx(20f), 0, 0, 0)

        title = if (messages.isNotEmpty())
            messages.size.toString()
        else ""

        val autoTransition = ChangeBounds()
        autoTransition.duration = 100
        TransitionManager.beginDelayedTransition(getChildAt(0) as ActionMenuView, autoTransition)
    }

    fun setMenuItemClickListener(listener: ((MenuItem) -> Unit)?) {
        itemClickListener = listener
    }

    fun setToolbarIconsVisibilityInitializer(listener: ToolbarActionsVisibilityListener) {
        visibilityInitializer = listener
    }

    override fun setVisibility(visibility: Int) {
        handledClick = false
        setVisibilityWithAnim(visibility == VISIBLE)
    }

    private fun setVisibilityWithAnim(visible: Boolean) {
        if (animation == null || !animation.hasStarted() || animation.hasEnded()) {
            if (visible) {
                if (!isVisible) {
                    alpha = 0f
                    super.setVisibility(VISIBLE)
                    changeAlphaWithAnim(1f, 300)
                }
            } else super.setVisibility(GONE)
        }
    }
}