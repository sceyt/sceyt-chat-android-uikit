package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.sceytchatuikit.presentation.common.diff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem

class AttachmentsDiffUtil(private var oldList: List<ChannelFileItem>,
                          private var newList: List<ChannelFileItem>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return if (oldItem !is ChannelFileItem.LoadingMoreItem && newItem !is ChannelFileItem.LoadingMoreItem)
            oldItem.file.id == newItem.file.id
        else oldItem == newItem
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return if (oldItem !is ChannelFileItem.LoadingMoreItem && newItem !is ChannelFileItem.LoadingMoreItem) {
            oldItem.file.diff(newItem.file).hasDifference().not()
        } else oldItem == newItem
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        if (oldItem !is ChannelFileItem.LoadingMoreItem && newItem !is ChannelFileItem.LoadingMoreItem)
            return oldItem.file.diff(newItem.file)
        return null
    }
}