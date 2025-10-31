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
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.isNotNullOrBlank

class MessageActionsPopupMenu(
        private val context: Context,
        anchor: View,
        private var message: SceytMessage,
) : PopupMenu(context, anchor) {

    @SuppressLint("RestrictedApi")
    override fun show() {
        inflate(R.menu.sceyt_menu_popup_message)
        (menu as MenuBuilder).setOptionalIconsVisible(true)
        gravity = if (message.incoming) GravityCompat.START else GravityCompat.END


        val isPending = message.deliveryStatus == DeliveryStatus.Pending

        menu.findItem(R.id.sceyt_reply).isVisible = !isPending
        menu.findItem(R.id.sceyt_forward).isVisible = !isPending

        val expiredEditMessage = (System.currentTimeMillis() - message.createdAt) > SceytChatUIKit.config.messageEditTimeout
        menu.findItem(R.id.sceyt_edit_message).isVisible = message.body.isNotNullOrBlank() && !expiredEditMessage

        val deleteMessageItem = menu.findItem(R.id.sceyt_delete_message)

        if (message.incoming) {
            deleteMessageItem.isVisible = false
            menu.findItem(R.id.sceyt_edit_message)?.isVisible = false
        } else
            deleteMessageItem.apply {
                title = setColoredTitle(title.toString())
                icon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    ContextCompat.getColor(context, R.color.sceyt_color_warning), BlendModeCompat.SRC_ATOP)
            }

        // Handle poll-specific actions
        val poll = message.poll
        val isPollMessage = poll != null
        val hasVoted = poll?.ownVotes?.isNotEmpty() == true
        val canRetractVote = !isPending && poll?.allowVoteRetract == true && hasVoted && !poll.closed
        val canEndVote = !isPending && !message.incoming && isPollMessage && !poll.closed

        menu.findItem(R.id.sceyt_retract_vote)?.isVisible = canRetractVote
        menu.findItem(R.id.sceyt_end_vote)?.isVisible = canEndVote

        super.show()
    }

    private fun setColoredTitle(text: String): SpannableString {
        val headerTitle = SpannableString(text)
        headerTitle.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.sceyt_color_warning)), 0, headerTitle.length, 0)
        return headerTitle
    }
}