package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.popups

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
import com.sceyt.sceytchatuikit.R

class PopupMenuMessage(private val context: Context, anchor: View, private var incoming: Boolean) : PopupMenu(context, anchor) {

    @SuppressLint("RestrictedApi")
    override fun show() {
        inflate(R.menu.sceyt_menu_popup_message)
        (menu as MenuBuilder).setOptionalIconsVisible(true)
        val deleteMessageItem = menu.findItem(R.id.sceyt_delete_message)
        if (incoming) {
            deleteMessageItem.isVisible = false
            menu.findItem(R.id.sceyt_edit_message)?.isVisible = false
        } else
            deleteMessageItem.apply {
                title = setColoredTitle(title.toString())
                icon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    ContextCompat.getColor(context, R.color.sceyt_color_red), BlendModeCompat.SRC_ATOP)
            }
        super.show()
    }

    private fun setColoredTitle(text: String): SpannableString {
        val headerTitle = SpannableString(text)
        headerTitle.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.sceyt_color_red)), 0, headerTitle.length, 0)
        return headerTitle
    }
}