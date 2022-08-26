package com.sceyt.chat.ui.presentation.changerole.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.ItemChooseRoleBinding

class ChooseRoleAdapter(private var rolesList: ArrayList<RoleItem>,
                        private val clickListener: ChooseRoleListener) : RecyclerView.Adapter<ChooseRoleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChooseRoleViewHolder {
        val itemView = ItemChooseRoleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChooseRoleViewHolder(itemView, clickListener)
    }

    override fun onBindViewHolder(holder: ChooseRoleViewHolder, position: Int) {
        holder.bindTo(rolesList[position])
    }

    override fun getItemCount(): Int {
        return rolesList.size
    }

    fun getData() = rolesList

    fun interface ChooseRoleListener {
        fun onRoleClick(role: RoleItem)
    }
}