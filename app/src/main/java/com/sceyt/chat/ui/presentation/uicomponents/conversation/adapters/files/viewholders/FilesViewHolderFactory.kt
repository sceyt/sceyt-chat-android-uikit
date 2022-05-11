package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.RecyclerviewMessageImageItemBinding
import com.sceyt.chat.ui.databinding.RecyclerviewMessageVideoItemBinding
import com.sceyt.chat.ui.databinding.SceytUiFileItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem

class FilesViewHolderFactory(context: Context) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder {
        return when (viewType) {
            FileViewType.File.ordinal -> {
                MessageFileViewHolder(SceytUiFileItemBinding.inflate(layoutInflater, parent, false))
            }
            FileViewType.Image.ordinal -> {
                MessageImageViewHolder(RecyclerviewMessageImageItemBinding.inflate(layoutInflater, parent, false))
            }
            FileViewType.Video.ordinal -> {
                MessageVideoViewHolder(RecyclerviewMessageVideoItemBinding.inflate(layoutInflater, parent, false))
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