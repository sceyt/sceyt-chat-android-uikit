package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.metions

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.databinding.SceytItemMemberBinding
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder

class UserViewHolderFactory(context: Context, private val listeners: UsersAdapter.ClickListener) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<SceytMember> {
        return UserViewHolder(SceytItemMemberBinding.inflate(layoutInflater, parent, false),
            listeners)
    }
}