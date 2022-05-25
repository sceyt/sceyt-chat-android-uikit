package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.ui.databinding.SceytUiMessageFileItemBinding
import com.sceyt.chat.ui.databinding.SceytUiMessageImageItemBinding
import com.sceyt.chat.ui.databinding.SceytUiMessageVideoItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class FilesViewHolderFactory(context: Context, private val messageListeners: MessageClickListenersImpl) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder {
        return when (viewType) {
            FileViewType.File.ordinal -> {
                MessageFileViewHolder(SceytUiMessageFileItemBinding.inflate(layoutInflater, parent, false),
                    messageListeners)
            }
            FileViewType.Image.ordinal -> {
                MessageImageViewHolder(SceytUiMessageImageItemBinding.inflate(layoutInflater, parent, false),
                    messageListeners)
            }
            FileViewType.Video.ordinal -> {
                MessageVideoViewHolder(SceytUiMessageVideoItemBinding.inflate(layoutInflater, parent, false),
                    messageListeners)
            }
            else -> throw Exception("Not supported view type")
        }
    }

    fun getItemViewType(item: FileListItem): Int {
        return when (item) {
            is FileListItem.File -> FileViewType.File.ordinal
            is FileListItem.Image -> FileViewType.Image.ordinal
            is FileListItem.Video -> FileViewType.Video.ordinal
        }
    }

    enum class FileViewType {
        File, Image, Video
    }
}