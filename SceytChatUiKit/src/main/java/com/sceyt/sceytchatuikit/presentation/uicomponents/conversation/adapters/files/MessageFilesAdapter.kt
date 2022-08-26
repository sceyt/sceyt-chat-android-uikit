package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.presentation.customviews.SceytVideoControllerView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory

class MessageFilesAdapter(private val files: ArrayList<FileListItem>,
                          private var viewHolderFactory: FilesViewHolderFactory
) : RecyclerView.Adapter<com.sceyt.sceytchatuikit.presentation.common.BaseViewHolder<FileListItem>>() {

    val videoControllersList = arrayListOf<SceytVideoControllerView>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): com.sceyt.sceytchatuikit.presentation.common.BaseViewHolder<FileListItem> {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: com.sceyt.sceytchatuikit.presentation.common.BaseViewHolder<FileListItem>, position: Int) {
        holder.bind(files[position])
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