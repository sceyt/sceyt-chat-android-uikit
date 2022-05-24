package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import com.sceyt.chat.ui.databinding.SceytUiMessageVideoItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem


class MessageVideoViewHolder(
        private val binding: SceytUiMessageVideoItemBinding,
) : BaseFileViewHolder(binding.root) {

    override fun bindTo(item: FileListItem) {
        binding.apply {
            loadData = item.fileLoadData

            parentLayout.clipToOutline = true

            setUploadListenerIfNeeded(item)

            item.downloadSuccess = { result ->
                videoView.setVideoPath(result.path)
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
                videoView.start()
            }

            downloadIfNeeded(item)

            root.setOnClickListener {
                openFile(item, itemView.context)
            }
        }
    }
}