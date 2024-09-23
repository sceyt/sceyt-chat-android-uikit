package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders

import android.view.View
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.AttachmentDataItem

abstract class BaseMessageFileViewHolder<Item : AttachmentDataItem>(
        itemView: View,
        needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseFileViewHolder<Item>(itemView, needMediaDataCallback) {
    protected lateinit var message: SceytMessage

    final override fun bind(item: Item) {
        throw Exception("Use bind(item: FileListItem, message: SceytMessage) instead")
    }

    open fun bind(item: Item, message: SceytMessage) {
        this.message = message
        super.bind(item)
    }
}