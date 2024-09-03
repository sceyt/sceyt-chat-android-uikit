package com.sceyt.chatuikit.presentation.uicomponents.messageinfo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.databinding.SceytItemUserMarkerBinding
import com.sceyt.chatuikit.extensions.getPresentableName
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
                val user = marker.user ?: User(marker.userId)
                val name = SceytChatUIKit.formatters.userNameFormatter?.format(user)
                        ?: user.getPresentableName()
                avatar.setNameAndImageUrl(name, user.avatarURL)
                userName.text = name
                tvData.text = DateTimeUtil.getDateTimeString(marker.createdAt, "dd.MM.yy • HH:mm")
            }
        }
    }
}