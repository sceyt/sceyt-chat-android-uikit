package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.content.res.ColorStateList
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelFileBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle

class FileViewHolder(private val binding: SceytItemChannelFileBinding,
                     private val clickListeners: AttachmentClickListenersImpl,
                     private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseFileViewHolder<ChannelFileItem>(binding.root, needMediaDataCallback) {

    init {
        binding.setupStyle()
        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }

        binding.loadProgress.setOnClickListener {
            clickListeners.onAttachmentLoaderClick(it, fileItem)
        }
    }

    override fun bind(item: ChannelFileItem) {
        super.bind(item)
        val file = (item as? ChannelFileItem.File)?.file ?: return

        with(binding) {
            tvFileName.text = file.name
            tvFileSizeAndDate.text = file.fileSize.toPrettySize()
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)

        if (data.state == TransferState.PendingDownload)
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
    }

    override val loadingProgressView: SceytCircularProgressView
        get() = binding.loadProgress

    private fun SceytItemChannelFileBinding.setupStyle() {
        icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
        icFile.backgroundTintList = ColorStateList.valueOf(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        loadProgress.setIconTintColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        loadProgress.setProgressColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}