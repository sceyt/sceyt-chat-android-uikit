package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files

import android.util.Log
import android.view.ViewGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory

class MessageFilesAdapter(private val files: ArrayList<FileListItem>,
                          private var viewHolderFactory: FilesViewHolderFactory
) : RecyclerView.Adapter<BaseViewHolder<FileListItem>>() {

    val hashMapPlayers = arrayListOf<ExoPlayer>()
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

    override fun onViewAttachedToWindow(holder: BaseViewHolder<FileListItem>) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder<FileListItem>) {
        super.onViewDetachedFromWindow(holder)
        Log.i("sdfsfd", "onViewDetachedFromWindow")

    }

    fun onItemDetached() {
        hashMapPlayers.forEach { it.release() }
        hashMapPlayers.clear()
        println("onItemcdfdfDetached")
    }
}