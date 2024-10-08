package com.sceyt.chatuikit.presentation.components.message_info.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytItemUserMarkerBinding
import com.sceyt.chatuikit.presentation.extensions.setUserAvatar
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

class UserMarkerAdapter : ListAdapter<SceytMarker, UserMarkerAdapter.SimpleUserViewHolder>(DIFF_UTIL) {

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<SceytMarker>() {
            override fun areItemsTheSame(oldItem: SceytMarker, newItem: SceytMarker): Boolean {
                return oldItem.userId == newItem.userId
            }

            override fun areContentsTheSame(oldItem: SceytMarker, newItem: SceytMarker): Boolean {
                return oldItem.userId == newItem.userId
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleUserViewHolder {
        return SimpleUserViewHolder(SceytItemUserMarkerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SimpleUserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SimpleUserViewHolder(
            private val binding: SceytItemUserMarkerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(marker: SceytMarker) {
            with(binding) {
                val user = marker.user ?: SceytUser(marker.userId)
                val name = SceytChatUIKit.formatters.userNameFormatter.format(itemView.context, user)
                avatar.setUserAvatar(marker.user)
                userName.text = name
                tvData.text = DateTimeUtil.getDateTimeString(marker.createdAt, "dd.MM.yy • HH:mm")
            }
        }
    }
}