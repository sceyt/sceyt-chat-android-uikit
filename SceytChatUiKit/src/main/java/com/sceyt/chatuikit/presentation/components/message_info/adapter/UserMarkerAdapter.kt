package com.sceyt.chatuikit.presentation.components.message_info.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.databinding.SceytItemUserMarkerBinding
import com.sceyt.chatuikit.styles.MessageInfoItemStyle

class UserMarkerAdapter(
        private val itemStyle: MessageInfoItemStyle,
) : ListAdapter<SceytMarker, UserMarkerViewHolder>(DIFF_UTIL) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserMarkerViewHolder {
        return UserMarkerViewHolder(
            binding = SceytItemUserMarkerBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            itemStyle = itemStyle
        )
    }

    override fun onBindViewHolder(holder: UserMarkerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}