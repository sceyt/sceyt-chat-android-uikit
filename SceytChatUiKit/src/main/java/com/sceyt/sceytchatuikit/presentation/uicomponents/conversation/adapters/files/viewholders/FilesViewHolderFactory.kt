package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.sceytchatuikit.databinding.SceytMessageFileItemBinding
import com.sceyt.sceytchatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.sceytchatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class FilesViewHolderFactory(context: Context, private val messageListeners: MessageClickListenersImpl?,
                             private val needDownloadCallback: (FileListItem) -> Unit) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder {
        return when (viewType) {
            FileViewType.File.ordinal -> {
                MessageFileViewHolder(SceytMessageFileItemBinding.inflate(layoutInflater, parent, false),
                    messageListeners, needDownloadCallback)
            }
            FileViewType.Image.ordinal -> {
                MessageImageViewHolder(SceytMessageImageItemBinding.inflate(layoutInflater, parent, false),
                    messageListeners, needDownloadCallback)
            }
            FileViewType.Video.ordinal -> {
                MessageVideoViewHolder(SceytMessageVideoItemBinding.inflate(layoutInflater, parent, false),
                    messageListeners, needDownloadCallback)
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