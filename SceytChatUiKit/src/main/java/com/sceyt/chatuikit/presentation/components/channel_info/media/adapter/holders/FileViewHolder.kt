package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders

import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelFileBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.extensions.toPrettySize
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.customviews.CircularProgressView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.chatuikit.styles.MessagesListViewStyle

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

        when (data.state) {
            TransferState.PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
            }
            TransferState.Downloaded -> {
                binding.icFile.setImageDrawable(MessagesListViewStyle.currentStyle?.messageItemStyle?.fileAttachmentIcon
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_file_filled))
            }
            else -> return
        }
    }

    override val loadingProgressView: CircularProgressView
        get() = binding.loadProgress

    private fun SceytItemChannelFileBinding.applyStyle() {
        val colorOnPrimary = context.getCompatColor(SceytChatUIKit.theme.onPrimaryColor)
        root.setBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.backgroundColorSections))
        icFile.setBackgroundTintColorRes(SceytChatUIKit.theme.accentColor)
        loadProgress.setIconTintColor(colorOnPrimary)
        loadProgress.setProgressColor(colorOnPrimary)
    }
}