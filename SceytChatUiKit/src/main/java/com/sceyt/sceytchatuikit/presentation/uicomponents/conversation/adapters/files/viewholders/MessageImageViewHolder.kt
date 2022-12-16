package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle


class MessageImageViewHolder(
        private val binding: SceytMessageImageItemBinding,
        private val messageListeners: MessageClickListenersImpl?,
        private val needDownloadCallback: (FileListItem) -> Unit) : BaseFileViewHolder(binding.root) {

    init {
        binding.setupStyle()

        binding.root.setOnClickListener {
            messageListeners?.onAttachmentClick(it, fileItem)
        }

        binding.root.setOnLongClickListener {
            messageListeners?.onAttachmentLongClick(it, fileItem)
            return@setOnLongClickListener true
        }

        binding.loadProgress.setOnClickListener {
            messageListeners?.onAttachmentLoaderClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        listenerKey = getKey()

        setListener()

        binding.loadProgress.release()
        transferData?.let {
            updateState(it)
            if (it.state == Downloading)
                needDownloadCallback.invoke(fileItem)
        }
    }

    private fun updateState(data: TransferData) {
        Log.i(TAG, "$data  $isFileItemInitialized")

        if (isFileItemInitialized.not()) return
        transferData = data

        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            PendingUpload -> {
                loadImage(fileItem.file.filePath)
            }
            PendingDownload -> {
                binding.fileImage.setImageBitmap(null)
                needDownloadCallback.invoke(fileItem)
            }
            Downloading -> {
                binding.fileImage.setImageBitmap(null)
            }
            Uploading -> {
                loadImage(fileItem.file.filePath)
            }
            Uploaded, Downloaded -> {
                loadImage(fileItem.file.filePath)
            }
        }
    }

    private fun setListener() {
        MessageFilesAdapter.setListener(listenerKey, ::updateState)
    }

    private fun loadImage(path: String?) {
        Glide.with(itemView.context.applicationContext)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(binding.root.width, binding.root.height)
            .into(binding.fileImage)
    }

    private fun SceytMessageImageItemBinding.setupStyle() {
        loadProgress.setProgressColor(context.getCompatColor(MessagesStyle.mediaLoaderColor))
    }
}