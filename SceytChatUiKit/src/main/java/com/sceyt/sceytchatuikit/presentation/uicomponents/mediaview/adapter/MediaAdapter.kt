package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter

import android.media.MediaPlayer
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.extensions.dispatchUpdatesToSafety
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder

class MediaAdapter(
        private var attachments: ArrayList<MediaItem>,
        private val attachmentViewHolderFactory: MediaFilesViewHolderFactory,
) : RecyclerView.Adapter<BaseFileViewHolder<MediaItem>>() {
    private var mediaPlayers = mutableListOf<MediaPlayer>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder<MediaItem> {
        return attachmentViewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseFileViewHolder<MediaItem>, position: Int) {
        holder.bind(attachments[position])
    }

    override fun onViewAttachedToWindow(holder: BaseFileViewHolder<MediaItem>) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseFileViewHolder<MediaItem>) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    override fun getItemCount(): Int {
        return attachments.size
    }

    override fun getItemViewType(position: Int): Int {
        return attachmentViewHolderFactory.getItemViewType(attachments[position])
    }

    fun getLastMediaItem() = attachments.last()

    fun getFirstMediaItem() = attachments.first()

    fun getData() = attachments

    fun notifyUpdate(data: List<MediaItem>, recyclerView: RecyclerView) {
        val myDiffUtil = MediaDiffUtil(attachments, data)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        productDiffResult.dispatchUpdatesToSafety(recyclerView)
        this.attachments.clear()
        this.attachments.addAll(data)
    }

    fun addPrevItems(data: List<MediaItem>) {
        if (data.isEmpty()) return
        val items = data.toArrayList()
        if (attachments.size == 1 && attachments[0].file.id == 0L) {
            items.find { it.file.url == attachments[0].file.url }?.let {
                items.remove(it)
            }
        }
        if (items.isEmpty()) return
        attachments.addAll(0, items)
        notifyItemRangeInserted(0, items.size)
    }

    fun addNextItems(data: List<MediaItem>) {
        if (data.isEmpty()) return
        attachments.addAll(data)
        notifyItemRangeInserted(attachments.size - data.size, data.size)
    }

    fun pauseAllVideos() {
        mediaPlayers.forEach { it.pause() }
    }

    fun addMediaPlayer(mediaPlayer: MediaPlayer) {
        mediaPlayers.add(mediaPlayer)
    }
}