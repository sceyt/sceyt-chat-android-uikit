package com.sceyt.chat.demo.presentation.main.adapters

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.chatuikit.data.models.messages.SceytUser

class SceytUserDiffCallback : DiffUtil.ItemCallback<SceytUser>() {
    override fun areItemsTheSame(oldItem: SceytUser, newItem: SceytUser): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SceytUser, newItem: SceytUser): Boolean {
        return oldItem == newItem
    }
}