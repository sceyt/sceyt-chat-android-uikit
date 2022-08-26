package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter

import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytItemInputAttachmentBinding
import com.sceyt.sceytchatuikit.extensions.isEqualsVideoOrImage
import java.io.File

class AttachmentFileViewHolder(private val binding: SceytItemInputAttachmentBinding,
                               private val callbacks: Callbacks) : com.sceyt.sceytchatuikit.presentation.common.BaseViewHolder<AttachmentItem>(binding.root) {

    override fun bind(item: AttachmentItem) {
        with(binding.fileImage) {
            if (item.attachment.type.isEqualsVideoOrImage()) {
                Glide.with(this)
                    .load(File(item.attachment.url))
                    .override(400)
                    .into(this)
                setPadding(0)
                setBackgroundColor(Color.TRANSPARENT)
            } else {
                setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.sceyt_ic_file))
                setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.sceyt_color_accent))
                setPadding(40)
            }
        }

        itemView.setOnClickListener { callbacks.onRemoveItem(item) }
    }

    fun interface Callbacks {
        fun onRemoveItem(item: AttachmentItem)
    }
}