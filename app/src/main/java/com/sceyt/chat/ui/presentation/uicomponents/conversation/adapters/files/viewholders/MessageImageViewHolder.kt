package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.sceyt.chat.ui.databinding.SceytMessageImageItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl


class MessageImageViewHolder(
        private val binding: SceytMessageImageItemBinding,
        private val messageListeners: MessageClickListenersImpl?) : BaseFileViewHolder(binding.root) {

    override fun bind(item: FileListItem) {
        with(binding) {
            loadData = item.fileLoadData
            fileImage.setImageBitmap(null)

            setUploadListenerIfNeeded(item)
            downloadIfNeeded(item) { result ->
                Glide.with(root)
                    .load(result)
                    .transition(withCrossFade())
                    .override(root.width, root.height)
                    .into(fileImage)
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
}