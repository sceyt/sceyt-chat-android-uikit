package com.sceyt.chatuikit.presentation.components.channel.messages.popups

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.GravityCompat
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.extensions.haveDeleteAnyMessagePermission
import com.sceyt.chatuikit.persistence.extensions.haveDeleteOwnMessagePermission
import com.sceyt.chatuikit.persistence.extensions.haveEditAnyMessagePermission
import com.sceyt.chatuikit.persistence.extensions.haveEditOwnMessagePermission
import com.sceyt.chatuikit.persistence.extensions.haveForwardMessagePermission
import com.sceyt.chatuikit.persistence.extensions.haveReplyMessagePermission

class MessageActionsPopupMenu(
        private val context: Context,
        anchor: View,
        private val message: SceytMessage,
        private val channel: SceytChannel
) : PopupMenu(context, anchor) {

    @SuppressLint("RestrictedApi")
    override fun show() {
        inflate(R.menu.sceyt_menu_popup_message)
        (menu as MenuBuilder).setOptionalIconsVisible(true)
        gravity = if (message.incoming) GravityCompat.START else GravityCompat.END

        val isPending = message.deliveryStatus == DeliveryStatus.Pending
        val expiredEditMessage = (System.currentTimeMillis() - message.createdAt) > SceytChatUIKit.config.messageEditTimeout

        val editMessage = menu.findItem(R.id.sceyt_edit_message)
        val deleteMessage = menu.findItem(R.id.sceyt_delete_message)
        val replyMessage = menu.findItem(R.id.sceyt_reply)
        val forwardMessage = menu.findItem(R.id.sceyt_forward)
        val replyInThread = menu.findItem(R.id.sceyt_reply_in_thread)
        val messageInfo = menu.findItem(R.id.sceyt_message_info)
        val copyMessage = menu.findItem(R.id.sceyt_copy_message)

        if (!message.incoming) {
            deleteMessage.apply {
                title = setColoredTitle(title.toString())
                icon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    ContextCompat.getColor(context, R.color.sceyt_color_warning), BlendModeCompat.SRC_ATOP)
            }
        }

        replyMessage.isVisible = !isPending && channel.haveReplyMessagePermission()
        forwardMessage.isVisible = !isPending && channel.haveForwardMessagePermission()
        editMessage.isVisible = when {
            message.body.isBlank() || expiredEditMessage -> false
            message.incoming -> channel.haveEditAnyMessagePermission()
            else -> channel.haveEditOwnMessagePermission() || channel.haveEditAnyMessagePermission()
        }
        deleteMessage.isVisible = when {
            message.incoming -> channel.haveDeleteAnyMessagePermission()
            else -> channel.haveDeleteOwnMessagePermission() || channel.haveDeleteAnyMessagePermission()
        }

        super.show()
    }

    private fun setColoredTitle(text: String): SpannableString {
        val headerTitle = SpannableString(text)
        headerTitle.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.sceyt_color_warning)), 0, headerTitle.length, 0)
        return headerTitle
    }
}