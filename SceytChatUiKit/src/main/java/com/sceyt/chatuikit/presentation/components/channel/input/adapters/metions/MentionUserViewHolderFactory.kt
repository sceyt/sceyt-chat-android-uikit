package com.sceyt.chatuikit.presentation.components.channel.input.adapters.metions

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytItemMemberBinding
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.input.MentionUsersListStyle

class MentionUserViewHolderFactory(
        context: Context,
        private val style: MentionUsersListStyle,
        private val listeners: UsersAdapter.ClickListener
) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup): BaseViewHolder<SceytMember> {
        return MentionUserViewHolder(
            binding = SceytItemMemberBinding.inflate(layoutInflater, parent, false),
            style = style,
            itemClickListener = listeners
        )
    }
}