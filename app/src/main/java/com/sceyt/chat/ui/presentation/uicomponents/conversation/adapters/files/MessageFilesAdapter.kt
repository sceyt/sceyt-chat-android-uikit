package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.presentation.customviews.VideoControllerView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory

class MessageFilesAdapter(private val files: ArrayList<FileListItem>,
                          private var viewHolderFactory: FilesViewHolderFactory
) : RecyclerView.Adapter<BaseViewHolder<FileListItem>>() {

    val videoControllersList = arrayListOf<VideoControllerView>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<FileListItem> {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<FileListItem>, position: Int) {
        holder.bindViews(files[position])
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(files[position])
    }

    override fun getItemCount(): Int {
        return files.size
    }

    fun onItemDetached() {
        videoControllersList.forEach { it.release() }
        videoControllersList.clear()
    }
}