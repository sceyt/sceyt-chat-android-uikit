package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem

sealed interface MemberClickListeners {

    fun interface MemberLongClickListener : MemberClickListeners {
        fun onMemberLongClick(view: View, item: MemberItem.Member)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners : MemberLongClickListener
}