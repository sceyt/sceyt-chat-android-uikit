package com.sceyt.chatuikit.presentation.components.global_search.files.adapter.holders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelFileBinding
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.channel_info.files.ChannelInfoFileItemStyle
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle

open class FilesSearchItemViewHolder(
    private val style: ChannelInfoFileItemStyle,
    private val binding: SceytItemChannelFileBinding,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val onAttachmentClickListener: ((GlobalSearchListItem.AttachmentItem) -> Unit)?,
    private val onAttachmentLoaderClickListener: ((GlobalSearchListItem.AttachmentItem) -> Unit)? = null,
) : BaseFileViewHolder<GlobalSearchListItem.AttachmentItem>(binding.root, needMediaDataCallback) {

    init {
        binding.applyStyle()
        binding.root.setOnClickListener {
            onAttachmentClickListener?.invoke(fileItem)
        }
        binding.loadProgress.setOnClickListener {
            onAttachmentLoaderClickListener?.invoke(fileItem)
        }
    }

    override fun bind(item: GlobalSearchListItem.AttachmentItem) {
        super.bind(item)
        val attachment = item.attachment
        binding.tvFileName.text = style.fileNameFormatter.format(context, attachment)
        binding.tvFileSizeAndDate.text = style.subtitleFormatter.format(context, attachment)
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        when (data.state) {
            TransferState.PendingDownload -> {
                binding.icFile.setImageResource(0)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
            }

            TransferState.Downloaded, TransferState.Uploaded -> {
                val icon = style.iconProvider.provide(context, fileItem.attachment)
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
