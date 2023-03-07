package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Size
import android.view.View
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.presentation.root.AttachmentViewHolderHelper
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.AttachmentDataItem

abstract class BaseFileViewHolder<Item : AttachmentDataItem>(itemView: View,
                                                             private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseViewHolder<Item>(itemView) {
    protected lateinit var fileItem: Item
    protected val viewHolderHelper by lazy { AttachmentViewHolderHelper(itemView) }

    override fun bind(item: Item) {
        fileItem = item
        viewHolderHelper.bind(item)
    }

    protected fun requestThumb() {
        itemView.post {
            if (fileItem.file.filePath.isNullOrBlank()) return@post
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem.file, getThumbSize()))
        }
    }

    open fun getThumbSize() = Size(itemView.width, itemView.height)
}