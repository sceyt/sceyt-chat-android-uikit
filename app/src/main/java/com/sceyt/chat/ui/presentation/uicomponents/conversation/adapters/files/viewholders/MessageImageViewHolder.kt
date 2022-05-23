package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.sceyt.chat.ui.databinding.SceytUiMessageImageItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem


class MessageImageViewHolder(
        private val binding: SceytUiMessageImageItemBinding) : BaseFileViewHolder(binding.root) {


    override fun bindTo(item: FileListItem) {
        binding.loadData = item.fileLoadData

        binding.fileImage.setImageBitmap(null)

        setUploadListenerIfNeeded(item)
        downloadIfNeeded(item) { result, _ ->
            Glide.with(binding.root)
                .load(result)
                .transition(withCrossFade())
                .override(binding.root.width, binding.root.height)
                .into(binding.fileImage)
        }

        binding.root.setOnClickListener {
            openFile(item, itemView.context)
        }
    }
}