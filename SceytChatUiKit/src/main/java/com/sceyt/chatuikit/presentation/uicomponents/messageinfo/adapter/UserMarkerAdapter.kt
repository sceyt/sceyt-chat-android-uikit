package com.sceyt.chatuikit.presentation.uicomponents.messageinfo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.Marker
import com.sceyt.chatuikit.databinding.SceytItemUserMarkerBinding
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

class UserMarkerAdapter : ListAdapter<Marker, UserMarkerAdapter.SimpleUserViewHolder>(DIFF_UTIL) {

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<Marker>() {
            override fun areItemsTheSame(oldItem: Marker, newItem: Marker): Boolean {
                return oldItem.user.id == newItem.user.id
            }

            override fun areContentsTheSame(oldItem: Marker, newItem: Marker): Boolean {
                return oldItem.user.id == newItem.user.id
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

        fun bind(marker: Marker) {
            with(binding) {
                val user = marker.user
                val name = SceytKitConfig.userNameBuilder?.invoke(user)
                        ?: user.getPresentableName()
                avatar.setNameAndImageUrl(name, user.avatarURL)
                userName.text = name
                tvData.text = DateTimeUtil.getDateTimeString(marker.createdAt, "dd.MM.yy • HH:mm")
            }
        }
    }
}