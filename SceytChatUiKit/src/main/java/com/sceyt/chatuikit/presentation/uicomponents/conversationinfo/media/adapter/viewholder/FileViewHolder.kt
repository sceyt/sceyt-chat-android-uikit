package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelFileBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.extensions.toPrettySize
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle

class FileViewHolder(private val binding: SceytItemChannelFileBinding,
                     private val clickListeners: AttachmentClickListenersImpl,
                     private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseFileViewHolder<ChannelFileItem>(binding.root, needMediaDataCallback) {

    init {
        binding.applyStyle()
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

    private fun SceytItemChannelFileBinding.applyStyle() {
        icFile.setImageDrawable(MessagesListViewStyle.currentStyle?.messageItemStyle?.fileAttachmentIcon
                ?: context.getCompatDrawable(R.drawable.sceyt_ic_file_filled))
        icFile.setBackgroundTintColorRes(SceytChatUIKit.theme.accentColor)
        loadProgress.setIconTintColor(context.getCompatColor(SceytChatUIKit.theme.accentColor))
        loadProgress.setProgressColor(context.getCompatColor(SceytChatUIKit.theme.accentColor))
    }
}