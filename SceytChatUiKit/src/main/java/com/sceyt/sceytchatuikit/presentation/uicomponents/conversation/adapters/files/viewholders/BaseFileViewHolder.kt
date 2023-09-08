package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Size
import android.view.View
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.ThumbData
import com.sceyt.sceytchatuikit.persistence.filetransfer.ThumbFor
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
        val thumbFromEnum = needThumbFor() ?: return
        itemView.post {
            if (fileItem.file.filePath.isNullOrBlank()) return@post
            val thumbData = ThumbData(thumbFromEnum.value, getThumbSize())
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem.file, thumbData))
        }
    }

    protected fun isValidThumb(data: ThumbData?): Boolean {
        return getThumbSize() == data?.size && needThumbFor()?.value == data.key
    }

    open fun needThumbFor(): ThumbFor? = null

    open fun getThumbSize() = Size(itemView.width, itemView.height)
}