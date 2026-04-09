package com.sceyt.chatuikit.presentation.common.recyclerview

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.differs.diff

class UserDiffUtilItemCallBack : DiffUtil.ItemCallback<SceytUser>() {

    override fun areItemsTheSame(oldItem: SceytUser, newItem: SceytUser): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SceytUser, newItem: SceytUser): Boolean {
        return oldItem.diff(newItem).hasDifference().not()
    }

    override fun getChangePayload(oldItem: SceytUser, newItem: SceytUser): Any {
        return oldItem.diff(newItem)
    }
}