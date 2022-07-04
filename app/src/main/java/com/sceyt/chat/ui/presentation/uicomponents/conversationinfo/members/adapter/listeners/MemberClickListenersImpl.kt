package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.listeners

import android.view.View
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.MemberItem

class MemberClickListenersImpl : MemberClickListeners.ClickListeners {
    private var moreClickListener: MemberClickListeners.MoreClickClickListener? = null

    override fun onMoreClick(view: View, item: MemberItem.Member) {
        moreClickListener?.onMoreClick(view, item)
    }

    fun setListener(listener: MemberClickListeners) {
        when (listener) {
            is MemberClickListeners.ClickListeners -> {
                moreClickListener = listener
            }
            is MemberClickListeners.MoreClickClickListener -> {
                moreClickListener = listener
            }
        }
    }
}