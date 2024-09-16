package com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.MemberItem
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.diff.MemberItemPayloadDiff

abstract class BaseMemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: MemberItem, diff: MemberItemPayloadDiff)
}