package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.databinding.ItemChannelVoiceBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseChannelFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil


class VoiceViewHolder(private var binding: ItemChannelVoiceBinding,
                      private val clickListener: AttachmentClickListenersImpl,
                      needMediaDataCallback: (NeedMediaInfoData) -> Unit)
    : BaseChannelFileViewHolder(binding.root, needMediaDataCallback) {


    init {
        binding.setupStyle()
        binding.root.setOnClickListener {
            clickListener.onAttachmentClick(it, item = fileItem)
        }
    }

    override fun bind(item: ChannelFileItem) {
        super.bind(item)
        val attachment = item.file

        with(binding) {
            tvFileName.text = (item as ChannelFileItem.Voice).data.user?.getPresentableName()
            tvDate.text = DateTimeUtil.getDateTimeString(attachment.createdAt, "dd.MM.yy • HH:mm")
            with(tvDuration) {
                fileItem.duration?.let {
                    text = DateTimeUtil.secondsToTime(it)
                    isVisible = true
                } ?: run { isVisible = false }
            }
        }
    }

    private fun ItemChannelVoiceBinding.setupStyle() {
        icFile.backgroundTintList = ColorStateList.valueOf(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}