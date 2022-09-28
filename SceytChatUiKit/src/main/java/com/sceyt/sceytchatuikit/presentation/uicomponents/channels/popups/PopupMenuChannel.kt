package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.popups

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel

class PopupMenuChannel(context: Context, anchor: View, private var channel: SceytChannel)
    : PopupMenu(context, anchor, Gravity.END) {

    @SuppressLint("RestrictedApi")
    override fun show() {
        inflate(R.menu.sceyt_menu_popup_channel)
        (menu as MenuBuilder).setOptionalIconsVisible(true)

        val isGroup = channel.isGroup
        menu.findItem(R.id.sceyt_leave_channel).isVisible = isGroup
        menu.findItem(R.id.sceyt_block_channel)?.isVisible = isGroup

        menu.findItem(R.id.sceyt_mark_as_read).isVisible = channel.unreadMessageCount > 0 || channel.markedUsUnread
        menu.findItem(R.id.sceyt_mark_as_unread).isVisible = channel.unreadMessageCount == 0L && !channel.markedUsUnread

        val isBlocked = isGroup.not() && (channel as? SceytDirectChannel)?.peer?.user?.blocked == true
        menu.findItem(R.id.sceyt_block_user)?.isVisible = isBlocked.not() && isGroup.not()
        menu.findItem(R.id.sceyt_un_block_user)?.isVisible = isBlocked && isGroup.not()
        super.show()
    }
}