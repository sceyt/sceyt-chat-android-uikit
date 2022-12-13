package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
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
    }

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        if (isFileItemInitialized)
            setListener()
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        if (isFileItemInitialized)
            fileItem.file.removeListener()
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        listenerKey = getKey()

        setListener()
        val transferData = item.file.fileTransferData

        if (transferData == null) {
            //  Log.i("sdfsdf", "filePath ${item.file.filePath}  url ${item.file.url}")
            if (item.file.filePath.isNullOrBlank() && item.file.url.isNotNullOrBlank()) {
                //  Log.i("sdfsdf", "needDownloadCallback")

                binding.fileImage.setImageBitmap(null)

                binding.loadProgress.isVisible = true
                needDownloadCallback.invoke(item)
            }
            return
        }
        updateState(transferData)
    }

    private fun updateState(transferData: TransferData) {
        //Log.i("sdfsdf22", "$transferData  $isFileItemInitialized")

        if (isFileItemInitialized.not()) return
        fileItem.file.fileTransferData = transferData
        when (transferData.state) {
            PendingUpload -> {
                binding.loadProgress.release()
                loadImage(fileItem.file.filePath)
                binding.loadProgress.isVisible = true
            }
            PendingDownload -> {
                needDownloadCallback.invoke(fileItem)
            }
            Downloading -> {
                binding.fileImage.setImageBitmap(null)
                binding.loadProgress.isVisible = true
                binding.loadProgress.setProgress(transferData.progressPercent)
            }
            Uploading -> {
                loadImage(fileItem.file.filePath)
                binding.loadProgress.isVisible = true
                binding.loadProgress.setProgress(transferData.progressPercent)
            }
            Uploaded -> {
                binding.loadProgress.isVisible = false
                loadImage(fileItem.file.filePath)
            }
            Downloaded -> {
                binding.loadProgress.isVisible = false
                loadImage(fileItem.file.fileTransferData?.filePath)
            }
            Error -> {

            }
        }
    }

    private fun setListener() {
        MessageFilesAdapter.setListener(listenerKey, ::updateState)
    }

    private fun loadImage(path: String?) {
        Glide.with(itemView.context)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(binding.root.width, binding.root.height)
            .into(binding.fileImage)
    }

    private fun SceytMessageImageItemBinding.setupStyle() {
        loadProgress.setProgressColor(context.getCompatColor(MessagesStyle.mediaLoaderColor))
    }
}