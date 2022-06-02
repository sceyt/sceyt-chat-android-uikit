package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chat.ui.databinding.SceytMessageVideoItemBinding
import com.sceyt.chat.ui.extensions.glideCustomTarget
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl


class MessageVideoViewHolder(
        private val binding: SceytMessageVideoItemBinding,
        private val messageListeners: MessageClickListenersImpl?,
) : BaseFileViewHolder(binding.root) {

    override fun bindViews(item: FileListItem) {
        with(binding) {
            loadData = item.fileLoadData
            parentLayout.clipToOutline = true
            videoView.isVisible = false

            setUploadListenerIfNeeded(item)
            downloadIfNeeded(item) { result ->
                val mediaPath = result.path
                initializePlayer(mediaPath)

                Glide.with(itemView.context)
                    .load(mediaPath)
                    .override(videoView.width, videoView.height)
                    .into(glideCustomTarget {
                        if (it != null) {
                            videoViewController.setImageThumb(it)
                        }
                    })
            }

            root.setOnClickListener {
                messageListeners?.onAttachmentClick(it, item)
            }

            root.setOnLongClickListener {
                messageListeners?.onAttachmentLongClick(it, item)
                return@setOnLongClickListener true
            }
        }
    }

    private fun initializePlayer(mediaPath: String) {
        binding.videoViewController.setPlayerViewAndPath(binding.videoView, mediaPath)
        (bindingAdapter as? MessageFilesAdapter)?.videoControllersList?.add(binding.videoViewController)
    }
}