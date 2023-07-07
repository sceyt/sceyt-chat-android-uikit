package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
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

    private fun initMenu(message: SceytMessage) {
        menu.forEach {
            it.icon?.setTint(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        }

        setOnMenuItemClickListener {
            handledClick = true
            itemClickListener?.invoke(it)
            return@setOnMenuItemClickListener true
        }

        val statusPendingOrFailed = message.deliveryStatus == DeliveryStatus.Pending

        menu.findItem(R.id.sceyt_reply).isVisible = !statusPendingOrFailed
        menu.findItem(R.id.sceyt_forward).isVisible = !statusPendingOrFailed
        menu.findItem(R.id.sceyt_edit_message).isVisible = !message.incoming && message.body.isNotNullOrBlank()
        menu.findItem(R.id.sceyt_copy_message).isVisible = message.body.isNotNullOrBlank()
    }

    fun setupMenuWithMessage(@MenuRes menuRes: Int, message: SceytMessage): Menu? {
        menu.clear()
        inflateMenu(menuRes).also {
            initMenu(message)
        }
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