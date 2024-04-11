package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem

sealed interface MemberClickListeners {

    fun interface MemberClickListener : MemberClickListeners {
        fun onMemberClick(view: View, item: MemberItem.Member)
    }

    fun interface MemberLongClickListener : MemberClickListeners {
        fun onMemberLongClick(view: View, item: MemberItem.Member)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners : MemberClickListener, MemberLongClickListener
}