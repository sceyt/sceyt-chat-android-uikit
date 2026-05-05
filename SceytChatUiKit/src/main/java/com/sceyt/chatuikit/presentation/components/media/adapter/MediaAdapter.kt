package com.sceyt.chatuikit.presentation.components.media.adapter

import android.content.Context
import android.os.PowerManager
import android.view.ViewGroup
import androidx.media3.common.Player
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.extensions.keepScreenOn
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.common.recyclerview.AsyncListDiffer
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import kotlinx.coroutines.CoroutineScope

class MediaAdapter(
        private val attachmentViewHolderFactory: MediaFilesViewHolderFactory,
        scope: CoroutineScope,
) : RecyclerView.Adapter<BaseFileViewHolder<MediaItem>>() {
    private var mediaPlayers = mutableListOf<Player>()
    private var wakeLock: PowerManager.WakeLock? = null
    var shouldPlayVideoPath: String? = null

    private val differ = AsyncListDiffer(
        adapter = this,
        diffCallback = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                oldItem.attachment.id == newItem.attachment.id

            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                oldItem.attachment.diff(newItem.attachment).hasDifference().not()

            override fun getChangePayload(oldItem: MediaItem, newItem: MediaItem) =
                oldItem.attachment.diff(newItem.attachment)
        },
        scope = scope,
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder<MediaItem> {
        return attachmentViewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseFileViewHolder<MediaItem>, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun onViewAttachedToWindow(holder: BaseFileViewHolder<MediaItem>) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseFileViewHolder<MediaItem>) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun getItemViewType(position: Int): Int =
        attachmentViewHolderFactory.getItemViewType(differ.currentList[position])

    fun getData() = differ.currentList

    fun submitList(data: List<MediaItem>, commitCallback: (() -> Unit)? = null) {
        differ.submitList(data, commitCallback)
    }

    fun pauseAllVideos() {
        mediaPlayers.forEach { it.pause() }
    }

    fun resumeLastVideo() {
        mediaPlayers.lastOrNull()?.playWhenReady = true
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