package com.sceyt.chatuikit.presentation.components.channel.messages.adapters

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem

class AttachmentsDiffUtil(
        private var oldList: List<ChannelFileItem>,
        private var newList: List<ChannelFileItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return if (oldItem is ChannelFileItem.Item && newItem is ChannelFileItem.Item)
            oldItem.attachment.id == newItem.attachment.id
        else oldItem == newItem
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return if (oldItem is ChannelFileItem.Item && newItem is ChannelFileItem.Item) {
            oldItem.attachment.diff(newItem.attachment).hasDifference().not()
        } else oldItem == newItem
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        if (oldItem is ChannelFileItem.Item && newItem is ChannelFileItem.Item)
            return oldItem.attachment.diff(newItem.attachment)
        return null
    }
}