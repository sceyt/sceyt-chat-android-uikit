package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelMediaDateBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.styles.ChannelInfoMediaStyle
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

class ChannelMediaDateViewHolder(
        private val binding: SceytItemChannelMediaDateBinding,
        private val style: ChannelInfoMediaStyle
) : BaseFileViewHolder<ChannelFileItem>(binding.root, {}) {

    init {
        binding.applyStyle()
    }

    override fun bind(item: ChannelFileItem) {
        val createdAt = (item as? ChannelFileItem.MediaDate)?.data?.attachment?.createdAt ?: return
        val date = DateTimeUtil.getDateTimeStringWithDateFormatter(
            context = itemView.context,
            time = createdAt,
            dateFormatter = style.mediaDateSeparatorFormat)
        binding.tvDate.text = date
    }

    private fun SceytItemChannelMediaDateBinding.applyStyle() {
        root.setBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor))
        tvDate.setTextColor(context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor))
    }
}
