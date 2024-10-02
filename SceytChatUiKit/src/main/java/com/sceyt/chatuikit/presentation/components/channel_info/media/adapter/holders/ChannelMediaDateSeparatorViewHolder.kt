package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders

import com.sceyt.chatuikit.databinding.SceytItemChannelMediaDateSeparatorBinding
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDateSeparatorStyle
import java.util.Date

class ChannelMediaDateSeparatorViewHolder(
        private val binding: SceytItemChannelMediaDateSeparatorBinding,
        private val style: ChannelInfoDateSeparatorStyle
) : BaseFileViewHolder<ChannelFileItem>(binding.root, {}) {

    init {
        binding.applyStyle()
    }

    override fun bind(item: ChannelFileItem) {
        val createdAt = (item as? ChannelFileItem.MediaDate)?.data?.attachment?.createdAt ?: return
        val date = style.dateFormatter.format(context, Date(createdAt))
        binding.tvDate.text = date
    }

    private fun SceytItemChannelMediaDateSeparatorBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.textStyle.apply(tvDate)
    }
}
