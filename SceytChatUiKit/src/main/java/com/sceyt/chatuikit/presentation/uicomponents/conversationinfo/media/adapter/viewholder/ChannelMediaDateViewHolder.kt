package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelMediaDateBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.chatuikit.sceytstyles.ConversationInfoMediaStyle
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

class ChannelMediaDateViewHolder(
        private val binding: SceytItemChannelMediaDateBinding,
        private val style: ConversationInfoMediaStyle
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
        root.setBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.backgroundColor))
        tvDate.setTextColor(context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))
    }
}
