package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import com.sceyt.sceytchatuikit.databinding.SceytItemChannelMediaDateBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import java.util.Date

class ChannelMediaDateViewHolder(private val binding: SceytItemChannelMediaDateBinding) : BaseFileViewHolder<ChannelFileItem>(binding.root, {}) {

    override fun bind(item: ChannelFileItem) {
        val createdAt = (item as? ChannelFileItem.MediaDate)?.data?.attachment?.createdAt ?: return
        binding.tvDate.text = DateTimeUtil.convertDateToString(Date(createdAt), "MMMM dd")
    }
}
