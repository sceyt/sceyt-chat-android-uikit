package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelFileBinding
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.channel_info.files.ChannelInfoFileItemStyle
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle

class FileViewHolder(
        private val binding: SceytItemChannelFileBinding,
        private val style: ChannelInfoFileItemStyle,
        private val clickListeners: AttachmentClickListeners.ClickListeners,
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
            tvFileName.text = style.fileNameFormatter.format(context, file)
            tvFileSizeAndDate.text = style.subtitleFormatter.format(context, file)
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)

        when (data.state) {
            TransferState.PendingDownload -> {
                binding.icFile.setImageResource(0)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
            }

            TransferState.Downloaded, TransferState.Uploaded -> {
                val icon = style.iconProvider.provide(context, fileItem.file)
                binding.icFile.setImageDrawable(icon)
            }

            else -> binding.icFile.setImageResource(0)
        }
    }

    override val loadingProgressViewWithStyle: Pair<CircularProgressView, MediaLoaderStyle>
        get() = binding.loadProgress to style.mediaLoaderStyle

    private fun SceytItemChannelFileBinding.applyStyle() {
        icFile.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
        style.fileNameTextStyle.apply(tvFileName)
        style.subtitleTextStyle.apply(tvFileSizeAndDate)
        style.mediaLoaderStyle.apply(loadProgress)
    }
}