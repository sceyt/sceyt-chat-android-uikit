package com.sceyt.chat.ui.presentation.uicomponents.messageinput.attachments

import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.RecyclerviewAttachmentFileItemBinding
import com.sceyt.chat.ui.extensions.isEqualsVideoOrImage
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.adapter.AttachmentItem
import java.io.File

class AttachmentFileViewHolder(private val binding: RecyclerviewAttachmentFileItemBinding,
                               private val callbacks: Callbacks) : BaseViewHolder<AttachmentItem>(binding.root) {

    override fun bindViews(item: AttachmentItem) {
        with(binding.fileImage) {
            if (item.attachment.type.isEqualsVideoOrImage()) {
                Glide.with(this)
                    .load(File(item.attachment.url))
                    .override(400)
                    .into(this)
                setPadding(0)
                setBackgroundColor(Color.TRANSPARENT)
            } else {
                setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.ic_file))
                setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorPrimary))
                setPadding(40)
            }
        }

        itemView.setOnClickListener { callbacks.itemRemoved(item) }
    }

    interface Callbacks {
        fun itemRemoved(item: AttachmentItem) {}
    }
}