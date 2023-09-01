package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader

import android.content.Context
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.changeAlphaWithAnim
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners.HeaderUIElementsListener.ToolbarActionsVisibilityListener
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class MessageActionToolbar @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0)
    : Toolbar(context, attributeSet, defStyleAttr) {

    private var itemClickListener: ((MenuItem) -> Unit)? = null
    private var visibilityInitializer: ToolbarActionsVisibilityListener? = null

    @Volatile
    var handledClick: Boolean = false
        private set

    private fun initMenu(vararg messages: SceytMessage) {
        overflowIcon?.setTint(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        menu.forEach {
            it.icon?.setTint(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        }

        setOnMenuItemClickListener {
            handledClick = true
            itemClickListener?.invoke(it)
            return@setOnMenuItemClickListener true
        }

        visibilityInitializer?.onInitToolbarActionsVisibility(*messages, menu = menu)
    }

    fun setupMenuWithMessages(@MenuRes menuRes: Int, vararg message: SceytMessage): Menu? {
        menu.clear()
        inflateMenu(menuRes).also {
            initMenu(*message)
        }

        val autoTransition = ChangeBounds()
        autoTransition.duration = 100
        TransitionManager.beginDelayedTransition(this.getChildAt(0) as ActionMenuView, autoTransition)
        return menu
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