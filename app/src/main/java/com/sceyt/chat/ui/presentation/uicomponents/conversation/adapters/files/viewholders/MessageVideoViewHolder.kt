package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sceyt.chat.ui.databinding.SceytUiMessageVideoItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl


class MessageVideoViewHolder(
        private val binding: SceytUiMessageVideoItemBinding,
        private val messageListeners: MessageClickListenersImpl,
) : BaseFileViewHolder(binding.root) {
    private var player: ExoPlayer? = null
    private var mediaPath: String? = null

    override fun bindViews(item: FileListItem) {
        with(binding) {
            loadData = item.fileLoadData

            parentLayout.clipToOutline = true

            setUploadListenerIfNeeded(item)

            item.downloadSuccess = { result ->
                mediaPath = result.path
                initializePlayer()

                /* videoView.setVideoPath(result.path)
                 videoView.setOnPreparedListener { mediaPlayer ->
                     val videoRatio = mediaPlayer.videoWidth / mediaPlayer.videoHeight.toFloat()
                     val screenRatio = binding.videoView.width / binding.videoView.height.toFloat()
                     val scaleX = videoRatio / screenRatio
                     if (scaleX >= 1f) {
                         binding.videoView.scaleX = scaleX
                     } else {
                         binding.videoView.scaleY = 1f / scaleX
                     }
                 }

                 videoView.isVisible = true
                 videoView.seekTo(1)*/
                // videoView.start()
            }

            downloadIfNeeded(item)

            root.setOnClickListener {
                messageListeners.onAttachmentClick(it, item)
                openFile(item, itemView.context)
            }

            root.setOnLongClickListener {
                messageListeners.onAttachmentLongClick(it, item)
                return@setOnLongClickListener true
            }
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(binding.root.context)
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer
                binding.videoView.showController()
                setMediaItem(exoPlayer)
                (bindingAdapter as MessageFilesAdapter).hashMapPlayers.add(exoPlayer)
            }
    }

    private fun setMediaItem(player: Player) {
        if (mediaPath != null) {
            val mediaItem = MediaItem.fromUri(mediaPath!!)
            player.setMediaItem(mediaItem)
        }
    }
}