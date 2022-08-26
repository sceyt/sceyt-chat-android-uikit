package com.sceyt.chat.ui.presentation.changerole.adapter

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.ItemChooseRoleBinding

class ChooseRoleViewHolder(private val binding: ItemChooseRoleBinding,
                           private val clickListener: ChooseRoleAdapter.ChooseRoleListener) :
        RecyclerView.ViewHolder(binding.root) {

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