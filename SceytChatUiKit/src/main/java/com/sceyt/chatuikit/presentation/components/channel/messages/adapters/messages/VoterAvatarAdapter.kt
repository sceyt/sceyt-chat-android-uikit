package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytItemVoterAvatarBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.messages_list.item.PollStyle
import com.sceyt.chatuikit.styles.messages_list.item.VoterAvatarRendererAttributes

class VoterAvatarAdapter(
        private val pollStyle: PollStyle,
        private val bubbleBackgroundStyleProvider: () -> BackgroundStyle,
) : ListAdapter<SceytUser, VoterAvatarAdapter.VoterAvatarViewHolder>(
    DIFF_CALLBACK
) {
    private val overlap = 9.dpToPx()

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SceytUser>() {
            override fun areItemsTheSame(oldItem: SceytUser, newItem: SceytUser): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SceytUser, newItem: SceytUser): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: SceytUser, newItem: SceytUser): Any? {
                return Any()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoterAvatarViewHolder {
        val binding = SceytItemVoterAvatarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VoterAvatarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoterAvatarViewHolder, position: Int) {
        holder.itemView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = if (position > 0) {
                -overlap
            } else 0
        }
        holder.bind(getItem(position))
    }

    inner class VoterAvatarViewHolder(
            private val binding: SceytItemVoterAvatarBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: SceytUser) {
            pollStyle.voterAvatarRenderer.render(
                context = itemView.context,
                from = VoterAvatarRendererAttributes(
                    bubbleBackgroundStyle = bubbleBackgroundStyleProvider(),
                    voter = user
                ),
                style = pollStyle.voterAvatarStyle,
                avatarView = binding.avatar
            )
        }
    }
}

