package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter

import android.graphics.Color
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.databinding.SceytItemInputAttachmentBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil

class AttachmentFileViewHolder(private val binding: SceytItemInputAttachmentBinding,
                               private val callbacks: Callbacks) : BaseViewHolder<AttachmentItem>(binding.root) {

    override fun bind(item: AttachmentItem) {
        with(binding.fileImage) {
            if (item.attachment.type.isEqualsVideoOrImage()) {
                Glide.with(context)
                    .load(item.attachment.filePath)
                    .override(itemView.width)
                    .into(glideCustomTarget(onResourceReady = { resource, _ ->
                        setImageDrawable(resource)
                    }))
                setPadding(0)
                setBackgroundColor(Color.TRANSPARENT)

                if (item.attachment.type == AttachmentTypeEnum.Video.value()) {
                    val durationMillis = FileResizeUtil.getVideoDuration(context, item.attachment.filePath)
                            ?: 0
                    binding.tvDuration.apply {
                        if (durationMillis > 0)
                            text = DateTimeUtil.secondsToTime(durationMillis / 1000)
                        isVisible = durationMillis > 0
                    }
                } else binding.tvDuration.isVisible = false
            } else {
                setBackgroundColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
                setPadding(40)
                setImageDrawable(context.getCompatDrawable(R.drawable.sceyt_ic_file))
                binding.tvDuration.isVisible = false
            }
        }

        itemView.setOnClickListener { callbacks.onRemoveItem(item) }
    }

    fun interface Callbacks {
        fun onRemoveItem(item: AttachmentItem)
    }
}