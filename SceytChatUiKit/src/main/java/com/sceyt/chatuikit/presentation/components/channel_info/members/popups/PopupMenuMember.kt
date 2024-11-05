package com.sceyt.chatuikit.presentation.components.channel_info.members.popups

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.sceyt.chatuikit.R

class PopupMenuMember(private val context: Context, anchor: View) : PopupMenu(context, anchor) {

    override fun show() {
        inflate(R.menu.sceyt_menu_popup_member)
        menu.findItem(R.id.sceyt_kick_member).apply {
            title = setColoredTitle(title.toString())
        }
        menu.findItem(R.id.sceyt_block_and_kick_member).apply {
            title = setColoredTitle(title.toString())
        }
        super.show()
    }

    private fun setColoredTitle(text: String): SpannableString {
        val headerTitle = SpannableString(text)
        headerTitle.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.sceyt_color_warning)), 0, headerTitle.length, 0)
        return headerTitle
    }
}