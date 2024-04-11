package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import com.sceyt.chatuikit.databinding.SceytItemChannelMediaDateBinding
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.chatuikit.sceytstyles.ConversationInfoMediaStyle
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

class ChannelMediaDateViewHolder(private val binding: SceytItemChannelMediaDateBinding) : BaseFileViewHolder<ChannelFileItem>(binding.root, {}) {

    override fun bind(item: ChannelFileItem) {
        val createdAt = (item as? ChannelFileItem.MediaDate)?.data?.attachment?.createdAt ?: return
        val date = DateTimeUtil.getDateTimeStringWithDateFormatter(
            context = itemView.context,
            time = createdAt,
            dateFormatter = ConversationInfoMediaStyle.mediaDateSeparatorFormat)
        binding.tvDate.text = date
    }
}
