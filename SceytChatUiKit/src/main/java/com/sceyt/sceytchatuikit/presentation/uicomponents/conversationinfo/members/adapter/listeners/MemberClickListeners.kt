package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem

sealed interface MemberClickListeners {

    fun interface MoreClickClickListener : MemberClickListeners {
        fun onMoreClick(view: View, item: MemberItem.Member)
    }

    /** User this if you want to implement all callbacks */
    interface ClickListeners : MoreClickClickListener
}