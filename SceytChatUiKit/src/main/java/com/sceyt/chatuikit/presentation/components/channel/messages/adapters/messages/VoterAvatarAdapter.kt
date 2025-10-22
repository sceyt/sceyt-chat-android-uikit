package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytItemVoterAvatarBinding
import com.sceyt.chatuikit.presentation.custom_views.AvatarView

class VoterAvatarAdapter : RecyclerView.Adapter<VoterAvatarAdapter.VoterAvatarViewHolder>() {

    private val voters = mutableListOf<SceytUser>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoterAvatarViewHolder {
        val binding = SceytItemVoterAvatarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VoterAvatarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoterAvatarViewHolder, position: Int) {
        holder.bind(voters[position])
    }

    override fun getItemCount() = voters.size

    fun submitList(newVoters: List<SceytUser>) {
        val diffCallback = VoterDiffCallback(voters, newVoters)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        voters.clear()
        voters.addAll(newVoters)
        diffResult.dispatchUpdatesTo(this)
    }

    class VoterAvatarViewHolder(
            private val binding: SceytItemVoterAvatarBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: SceytUser) {
            binding.avatar.appearanceBuilder()
                .setImageUrl(user.avatarURL)
                .build()
                .applyToAvatar()
        }
    }

    private class VoterDiffCallback(
            private val oldList: List<SceytUser>,
            private val newList: List<SceytUser>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

