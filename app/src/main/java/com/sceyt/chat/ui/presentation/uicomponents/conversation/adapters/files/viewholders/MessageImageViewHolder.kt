package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.sceyt.chat.ui.databinding.SceytUiMessageImageItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl


class MessageImageViewHolder(
        private val binding: SceytUiMessageImageItemBinding,
        private val messageListeners: MessageClickListenersImpl) : BaseFileViewHolder(binding.root) {

    override fun bindViews(item: FileListItem) {
        with(binding) {
            loadData = item.fileLoadData
            fileImage.setImageBitmap(null)

            setUploadListenerIfNeeded(item)

            item.downloadSuccess = { result ->
                Glide.with(root)
                    .load(result)
                    .transition(withCrossFade())
                    .override(root.width, root.height)
                    .into(fileImage)
                Unit
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
}