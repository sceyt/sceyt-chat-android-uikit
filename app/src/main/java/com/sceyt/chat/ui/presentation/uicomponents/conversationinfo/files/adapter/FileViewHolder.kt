package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.files.adapter

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.ui.databinding.ItemChannelFileBinding

class FileViewHolder(private val binding: ItemChannelFileBinding) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var attachment: Attachment

    fun bind(data: Attachment) {
        this.attachment = data


    }
}