package com.sceyt.chat.demo.presentation.login.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.demo.databinding.ItemSelectedAccountBinding
import com.sceyt.chat.demo.presentation.main.adapters.SceytUserDiffCallback
import com.sceyt.chatuikit.data.models.messages.SceytUser

class SceytUsersAdapter(
    private val onItemClick: (SceytUser) -> Unit
) : ListAdapter<SceytUser, SceytUsersAdapter.SceytUserViewHolder>(SceytUserDiffCallback()) {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SceytUserViewHolder {
        val binding =
            ItemSelectedAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SceytUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SceytUserViewHolder, position: Int) {
        val user = getItem(position)
        val isSelected = position == selectedPosition
        holder.bind(user, isSelected, onItemClick)
    }

    inner class SceytUserViewHolder(private val binding: ItemSelectedAccountBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: SceytUser, isSelected: Boolean, onItemClick: (SceytUser) -> Unit) {
            binding.apply {
                tvUsername.text = user.id
                ivAvatar.setImageUrl(user.avatarURL)
                checkbox.isChecked = isSelected
                root.setOnClickListener {
                    selectItem(bindingAdapterPosition)
                    onItemClick(user)
                }
                checkbox.setOnClickListener {
                    selectItem(bindingAdapterPosition)
                    onItemClick(user)
                }
            }
        }

        private fun selectItem(position: Int) {
            if (selectedPosition == position) return
            val previousPosition = selectedPosition
            selectedPosition = position

            notifyItemChanged(previousPosition, Unit)
            notifyItemChanged(selectedPosition, Unit)
        }
    }
}
