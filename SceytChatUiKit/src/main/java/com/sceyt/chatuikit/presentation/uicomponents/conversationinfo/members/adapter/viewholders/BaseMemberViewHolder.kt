package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff

abstract class BaseMemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: MemberItem, diff: MemberItemPayloadDiff)
}