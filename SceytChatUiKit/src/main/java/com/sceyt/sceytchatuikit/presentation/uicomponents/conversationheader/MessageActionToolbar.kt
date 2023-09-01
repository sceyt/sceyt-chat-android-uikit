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
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.changeAlphaWithAnim
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class MessageActionToolbar @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0)
    : Toolbar(context, attributeSet, defStyleAttr) {

    private var itemClickListener: ((MenuItem) -> Unit)? = null

    @Volatile
    var handledClick: Boolean = false
        private set

    private fun initMenu(vararg messages: SceytMessage) {
        menu.forEach {
            it.icon?.setTint(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        }

        setOnMenuItemClickListener {
            handledClick = true
            itemClickListener?.invoke(it)
            return@setOnMenuItemClickListener true
        }

        val isSingleMessage = messages.size == 1
        val firstMessage = messages.getOrNull(0)

        firstMessage?.let { message ->
            menu.findItem(R.id.sceyt_reply).isVisible = isSingleMessage && message.deliveryStatus != DeliveryStatus.Pending
            menu.findItem(R.id.sceyt_edit_message).isVisible = isSingleMessage && !message.incoming && message.body.isNotNullOrBlank()
            menu.findItem(R.id.sceyt_copy_message).isVisible = messages.any { it.body.isNotNullOrBlank() }
        }
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