package com.sceyt.chat.ui.presentation.uicomponents.conversation.messageinput.attachments

import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.RecyclerviewAttachmentFileItemBinding
import com.sceyt.chat.ui.extensions.isEqualsVideoOrImage
import java.io.File


class AttachmentFileViewHolder(private val binding: RecyclerviewAttachmentFileItemBinding, private val callbacks: Callbacks) :
        AAttachmentViewHolder(binding.root) {

    override fun bindTo(attachment: Attachment?) {
        super.bindTo(attachment)

        if (attachment == null)
            return

        with(itemView) {

            with(binding.fileImage) {
                if (attachment.type.isEqualsVideoOrImage()) {
                    Glide.with(this)
                        .load(File(attachment.url))
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

            itemView.setOnClickListener { callbacks.itemRemoved(attachment) }
        }
    }

    interface Callbacks {
        fun itemRemoved(item: Attachment?) {}
    }
}