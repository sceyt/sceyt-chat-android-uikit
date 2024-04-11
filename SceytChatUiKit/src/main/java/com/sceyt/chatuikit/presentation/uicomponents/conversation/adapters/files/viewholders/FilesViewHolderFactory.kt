package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytMessageFileItemBinding
import com.sceyt.chatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.chatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners

class FilesViewHolderFactory(context: Context, private val messageListeners: MessageClickListeners.ClickListeners?,
                             private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder<FileListItem> {
        return when (viewType) {
            FileViewType.File.ordinal -> {
                MessageFileViewHolder(SceytMessageFileItemBinding.inflate(layoutInflater, parent, false),
                    messageListeners, needMediaDataCallback)
            }
            FileViewType.Image.ordinal -> {
                MessageImageViewHolder(SceytMessageImageItemBinding.inflate(layoutInflater, parent, false),
                    messageListeners, needMediaDataCallback)
            }
            FileViewType.Video.ordinal -> {
                MessageVideoViewHolder(SceytMessageVideoItemBinding.inflate(layoutInflater, parent, false),
                    messageListeners, needMediaDataCallback)
            }
            else -> throw RuntimeException("Not supported view type")
        }
    }

    fun getItemViewType(item: FileListItem): Int {
        return when (item) {
            is FileListItem.File -> FileViewType.File.ordinal
            is FileListItem.Image -> FileViewType.Image.ordinal
            is FileListItem.Video -> FileViewType.Video.ordinal
            else -> throw RuntimeException("Not supported view type")
        }
    }

    enum class FileViewType {
        File, Image, Video
    }
}