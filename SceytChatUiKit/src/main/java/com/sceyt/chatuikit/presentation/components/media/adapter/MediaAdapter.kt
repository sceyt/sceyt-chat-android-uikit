package com.sceyt.chatuikit.presentation.components.media.adapter

import android.content.Context
import android.os.PowerManager
import android.view.ViewGroup
import androidx.media3.common.Player
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.extensions.dispatchUpdatesToSafety
import com.sceyt.chatuikit.extensions.keepScreenOn
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder

class MediaAdapter(
        private var attachments: ArrayList<MediaItem>,
        private val attachmentViewHolderFactory: MediaFilesViewHolderFactory,
) : RecyclerView.Adapter<BaseFileViewHolder<MediaItem>>() {
    private var mediaPlayers = mutableListOf<Player>()
    private var wakeLock: PowerManager.WakeLock? = null
    var shouldPlayVideoPath: String? = null

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
        attachments = data.toArrayList()
    }

    fun addPrevItems(data: List<MediaItem>) {
        if (data.isEmpty()) return
        val items = data.toArrayList()
        if (attachments.size == 1 && attachments[0].attachment.id == 0L) {
            items.find { it.attachment.url == attachments[0].attachment.url }?.let {
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

    fun releaseAllPlayers() {
        mediaPlayers.forEach { it.release() }
    }

    fun addMediaPlayer(mediaPlayer: Player?) {
        mediaPlayer?.let { mediaPlayers.add(it) }
    }

    fun initWakeLock(context: Context) {
        if (wakeLock == null)
            wakeLock = context.keepScreenOn()

        if (wakeLock?.isHeld == false)
            wakeLock?.acquire(30 * 60 * 1000L /*30 minutes*/)
    }

    fun releaseWakeLock() {
        if (wakeLock?.isHeld == true)
            wakeLock?.release()
    }
}