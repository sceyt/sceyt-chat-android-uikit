package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ItemChannelMediaBinding

class MediaViewHolder(private val binding: ItemChannelMediaBinding) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var attachment: Attachment

    fun bind(data: Attachment) {
        this.attachment = data

        Glide.with(itemView.context)
            .load(attachment.url)
            .override(binding.icImage.width)
            .placeholder(R.color.sceyt_color_gray)
            .into(binding.icImage)
    }
}