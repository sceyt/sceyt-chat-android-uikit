package com.sceyt.chatuikit.presentation.components.message_info.adapter

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytItemUserMarkerBinding
import com.sceyt.chatuikit.styles.MessageInfoItemStyle
import java.util.Date

class UserMarkerViewHolder(
        private val binding: SceytItemUserMarkerBinding,
        private val itemStyle: MessageInfoItemStyle,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.applyStyle()
    }

    fun bind(marker: SceytMarker) {
        with(binding) {
            val user = marker.user ?: SceytUser(marker.userId)
            itemStyle.avatarRenderer.render(itemView.context, user, itemStyle.avatarStyle, avatar)
            userName.text = itemStyle.titleFormatter.format(itemView.context, user)
            tvData.text = itemStyle.subtitleFormatter.format(itemView.context, Date(marker.createdAt))
        }
    }

    private fun SceytItemUserMarkerBinding.applyStyle() {
        itemStyle.titleTextStyle.apply(userName)
        itemStyle.subtitleTextStyle.apply(tvData)
        itemStyle.avatarStyle.apply(avatar)
    }
}