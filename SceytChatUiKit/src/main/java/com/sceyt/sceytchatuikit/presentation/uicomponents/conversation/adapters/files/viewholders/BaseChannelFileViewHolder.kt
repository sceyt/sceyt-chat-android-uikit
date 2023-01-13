package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Size
import android.view.View
import androidx.annotation.CallSuper
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.presentation.root.AttachmentViewHolderHelper
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem

abstract class BaseChannelFileViewHolder(itemView: View,
                                         private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseViewHolder<ChannelFileItem>(itemView) {
    protected lateinit var fileItem: ChannelFileItem
    protected val viewHolderHelper by lazy { AttachmentViewHolderHelper(itemView) }

    override fun bind(item: ChannelFileItem) {
        fileItem = item
        viewHolderHelper.bind(item)
    }

    protected fun requestThumb() {
        itemView.post {
            if (fileItem.file.filePath.isNullOrBlank()) return@post
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem.file, getThumbSize()))
        }
    }

    open fun getThumbSize() = Size(itemView.width / 2, itemView.height)
}