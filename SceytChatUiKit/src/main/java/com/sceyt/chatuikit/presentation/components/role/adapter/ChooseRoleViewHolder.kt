package com.sceyt.chatuikit.presentation.components.role.adapter

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemChooseRoleBinding

class ChooseRoleViewHolder(private val binding: SceytItemChooseRoleBinding,
                           private val clickListener: ChooseRoleAdapter.ChooseRoleListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bindTo(roleItem: RoleItem) {
        with(binding) {
            rbRole.text = roleItem.role.name
            rbRole.isChecked = roleItem.checked

            rbRole.setOnClickListener {
                if (roleItem.checked) return@setOnClickListener
                clickListener.onRoleClick(roleItem)
            }
        }
    }
}