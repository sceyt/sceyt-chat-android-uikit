package com.sceyt.chatuikit.presentation.components.channel.input.adapters.metions

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytItemMemberBinding
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class MentionUserViewHolderFactory(context: Context, private val listeners: UsersAdapter.ClickListener) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup): BaseViewHolder<SceytMember> {
        return MentionUserViewHolder(SceytItemMemberBinding.inflate(layoutInflater, parent, false),
            listeners)
    }
}