package com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.MemberItem

class MemberClickListenersImpl : MemberClickListeners.ClickListeners {
    private var memberClickListener: MemberClickListeners.MemberClickListener? = null
    private var memberLongClickListener: MemberClickListeners.MemberLongClickListener? = null

    override fun onMemberClick(view: View, item: MemberItem.Member) {
        memberClickListener?.onMemberClick(view, item)
    }

    override fun onMemberLongClick(view: View, item: MemberItem.Member) {
        memberLongClickListener?.onMemberLongClick(view, item)
    }

    fun setListener(listener: MemberClickListeners) {
        when (listener) {
            is MemberClickListeners.ClickListeners -> {
                memberClickListener = listener
                memberLongClickListener = listener
            }

            is MemberClickListeners.MemberClickListener -> {
                memberClickListener = listener
            }

            is MemberClickListeners.MemberLongClickListener -> {
                memberLongClickListener = listener
            }
        }
    }
}