package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.files.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.ui.databinding.ItemChannelFileBinding

class ChannelFilesAdapter : ListAdapter<Attachment, FileViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder(ItemChannelFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
       // holder.bind(currentList[position])
    }

    override fun getItemCount(): Int {
        return 20
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Attachment>() {
            override fun areItemsTheSame(oldItem: Attachment, newItem: Attachment) =
                    oldItem.url == newItem.url

            override fun areContentsTheSame(oldItem: Attachment, newItem: Attachment) =
                    oldItem.url == newItem.url
        }
    }
}