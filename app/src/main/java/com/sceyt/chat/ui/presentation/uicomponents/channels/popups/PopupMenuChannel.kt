package com.sceyt.chat.ui.presentation.uicomponents.channels.popups

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extensions.getCompatColor

class PopupMenuChannel(private val context: Context, anchor: View, private var isGroup: Boolean)
    : PopupMenu(context, anchor, Gravity.RIGHT) {

    @SuppressLint("RestrictedApi")
    override fun show() {
        inflate(R.menu.sceyt_menu_popup_channel)
        (menu as MenuBuilder).setOptionalIconsVisible(true)

        menu.findItem(R.id.sceyt_clear_history).icon?.setTint(context.getCompatColor(R.color.sceyt_color_accent))
        menu.findItem(R.id.sceyt_leave_channel).isVisible = isGroup
        menu.findItem(R.id.sceyt_block_channel)?.isVisible = isGroup
        menu.findItem(R.id.sceyt_block_user)?.isVisible = !isGroup
        super.show()
    }
}