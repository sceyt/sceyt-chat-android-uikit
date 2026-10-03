package com.sceyt.chatuikit.presentation.components.channel.header

import android.content.Context
import android.view.Menu
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.presentation.extensions.isDeletedOrHardDeleted
import com.sceyt.chatuikit.presentation.extensions.isDisappearing
import com.sceyt.chatuikit.presentation.extensions.isPending
import com.sceyt.chatuikit.presentation.extensions.isSupportedType

/**
 * Which message actions apply to a selection, for every screen that offers them — the
 * conversation header and the pinned-messages screen.
 *
 * Shared rather than duplicated so the two screens cannot drift: a message that can be edited
 * in the chat can be edited from the pinned list, and a rule added here reaches both.
 */
internal object MessageActionsMenuInitializer {

    fun init(context: Context, menu: Menu, vararg messages: SceytMessage) {
        if (messages.isEmpty()) return

        fun Menu.setVisible(id: Int, visible: Boolean) {
            findItem(id)?.isVisible = visible
        }

        val isSingle = messages.size == 1
        val firstMessage = messages.first()
        val now = System.currentTimeMillis()

        val isUnsupportedFirst = !firstMessage.isSupportedType()

        val anyPending = messages.any { it.isPending() }
        val anyPollInSelection = messages.any { it.type == SceytMessageType.Poll.value }
        val anyUnsupportedInSelection = messages.any { !it.isSupportedType() }
        val anyViewOnceMessage = messages.any { it.viewOnce }

        val poll = firstMessage.poll
        val isPollMessage = poll != null
        val pollClosed = poll?.closed == true
        val hasVoted = poll?.ownVotes?.isNotEmpty() == true
        val allowRetract = poll?.allowVoteRetract == true

        val editTimeoutMs = SceytChatUIKit.config.messageEditTimeout
        val editExpired = (now - firstMessage.createdAt) > editTimeoutMs
        val isOutgoing = !firstMessage.incoming
        val hasText = firstMessage.body.isNotNullOrBlank()

        val canReply = isSingle && !anyPending
        val canForward =
            !anyPending && !isPollMessage && !anyPollInSelection && !anyUnsupportedInSelection && !anyViewOnceMessage
        val canEdit =
            isSingle && !isUnsupportedFirst && isOutgoing && hasText && !editExpired && !isPollMessage && !anyViewOnceMessage
        val canShowInfo = isSingle && isOutgoing && !anyPending
        val canCopy =
            messages.any { it.body.isNotNullOrBlank() } && !anyPollInSelection && !anyUnsupportedInSelection && !anyViewOnceMessage

        val canRetractVote = !anyPending && isSingle && allowRetract && hasVoted && !pollClosed
        val canEndVote = !anyPending && isSingle && isOutgoing && isPollMessage && !pollClosed

        val isPinned = firstMessage.pinDetails?.isPinned == true
        // Unpin stays available even when the message no longer meets the pinning conditions
        // — an existing pin must always be liftable, or it could be stranded.
        val canUnpin = isSingle && isPinned
        val canPin = isSingle && !isPinned && !anyPending && !isUnsupportedFirst &&
                !firstMessage.isDisappearing() && !firstMessage.state.isDeletedOrHardDeleted()

        menu.setVisible(R.id.sceyt_reply, canReply)
        menu.setVisible(R.id.sceyt_forward, canForward)
        menu.setVisible(R.id.sceyt_edit_message, canEdit)
        menu.setVisible(R.id.sceyt_message_info, canShowInfo)
        menu.setVisible(R.id.sceyt_copy_message, canCopy)
        menu.setVisible(R.id.sceyt_retract_vote, canRetractVote)
        menu.setVisible(R.id.sceyt_end_vote, canEndVote)

        menu.setVisible(R.id.sceyt_pin_message, canPin || canUnpin)
        menu.findItem(R.id.sceyt_pin_message)?.let { item ->
            item.setTitle(if (canUnpin) R.string.sceyt_unpin else R.string.sceyt_pin)
            item.setIcon(
                if (canUnpin) R.drawable.sceyt_ic_un_pin else R.drawable.sceyt_ic_pin
            )
            item.contentDescription = context.getString(
                if (canUnpin) R.string.sceyt_cd_unpin_message else R.string.sceyt_cd_pin_message
            )
        }
    }
}
