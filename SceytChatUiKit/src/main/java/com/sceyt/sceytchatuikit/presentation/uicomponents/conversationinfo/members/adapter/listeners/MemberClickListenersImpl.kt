package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem

class MemberClickListenersImpl : MemberClickListeners.ClickListeners {
    private var moreClickListener: MemberClickListeners.MemberLongClickListener? = null

    override fun onMemberLongClick(view: View, item: MemberItem.Member) {
        moreClickListener?.onMemberLongClick(view, item)
    }

    fun setListener(listener: MemberClickListeners) {
        when (listener) {
            is MemberClickListeners.ClickListeners -> {
                moreClickListener = listener
            }
            is MemberClickListeners.MemberLongClickListener -> {
                moreClickListener = listener
            }
        }
    }
}