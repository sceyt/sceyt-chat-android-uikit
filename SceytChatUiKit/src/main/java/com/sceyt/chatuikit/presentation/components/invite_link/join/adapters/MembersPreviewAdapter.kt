package com.sceyt.chatuikit.presentation.components.invite_link.join.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytItemChannelInviteMemberBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelMoreInviteMemberBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.Shape

class MembersPreviewAdapter(
        private val memberCount: Int,
        private val maxPreviewCount: Int = 3,
) : ListAdapter<SceytMember, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    private val overlap = 18.dpToPx()

    enum class ViewType(val type: Int) {
        Member(0),
        More(1);
    }

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SceytMember>() {
            override fun areItemsTheSame(oldItem: SceytMember, newItem: SceytMember): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SceytMember, newItem: SceytMember): Boolean {
                return oldItem.avatarUrl == newItem.avatarUrl
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewType.Member.type -> {
                val binding = SceytItemChannelInviteMemberBinding.inflate(inflater, parent, false)
                MemberViewHolder(binding)
            }

            ViewType.More.type -> {
                val binding = SceytItemChannelMoreInviteMemberBinding.inflate(inflater, parent, false)
                MoreViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = if (position > 0) {
                -overlap
            } else 0
        }

        when (holder) {
            is MemberViewHolder -> holder.bind(currentList[position])
            is MoreViewHolder -> holder.bind()
        }
    }

    override fun getItemCount(): Int {
        return if (memberCount > maxPreviewCount) {
            // Show up to maxPreviewCount items (or less if not available) + the +x item
            minOf(currentList.size, maxPreviewCount) + 1
        } else {
            // Just show the items we have
            currentList.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        val membersToShow = minOf(currentList.size, maxPreviewCount)
        return if (memberCount > maxPreviewCount && position == membersToShow) {
            ViewType.More.type
        } else {
            ViewType.Member.type
        }
    }

    inner class MemberViewHolder(
            val binding: SceytItemChannelInviteMemberBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: SceytMember) {
            SceytChatUIKit.renderers.userAvatarRenderer.render(
                context = binding.root.context,
                from = member.user,
                style = AvatarStyle(shape = Shape.Circle),
                avatarView = binding.avatar
            )
        }
    }

    inner class MoreViewHolder(
            val binding: SceytItemChannelMoreInviteMemberBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind() {
            val shownMembers = minOf(currentList.size, maxPreviewCount)
            val moreCount = memberCount - shownMembers
            binding.tvCount.text = "+$moreCount"
        }
    }
}