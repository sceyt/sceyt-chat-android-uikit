package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Log
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.data.models.messages.FileLoadData
import com.sceyt.sceytchatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.filetransfer.ProgressState.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import java.io.File


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
        if (isFileItemInitialized.not()) return

        fileItem.file.setListener {
            updateState(it)
        }
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        if (isFileItemInitialized.not()) return
        fileItem.file.removeListener()
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        val transferData = item.file.fileTransferData
        Log.i("afsdfsdf", transferData.toString())

        if (transferData == null) {
            if (item.file.filePath.isNullOrBlank() && item.file.url.isNotNullOrBlank()) {
                needDownloadCallback.invoke(item)
                binding.loadProgress.isVisible = true
            } else
                binding.loadProgress.isVisible = false
            return
        }

        Log.i("sdfsdf", transferData.state.toString())
        updateState(transferData)

        //binding.fileImage.setImageBitmap(null)
    }

    private fun updateState(transferData: TransferData) {
        if (isFileItemInitialized.not()) return

        when (transferData.state) {
            PendingUpload -> {
                binding.loadProgress.release()
                if (!fileItem.sceytMessage.incoming) {

                    loadImage(fileItem.file.filePath)

                    binding.loadProgress.isVisible = true
                }
            }
            PendingDownload -> {
                needDownloadCallback.invoke(fileItem)
            }
            Downloading, Uploading -> {
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
            Error -> TODO()
        }
    }

    private fun loadImage(path: String?) {
        Glide.with(itemView.context)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(binding.root.width, binding.root.height)
            .into(binding.fileImage)
    }

    private fun SceytMessageImageItemBinding.updateDownloadState(data: FileLoadData, file: File?) {
        loadProgress.isVisible = data.loading
        loadProgress.setProgress(data.progressPercent)
        if (file != null) {
            Glide.with(itemView.context)
                .load(file)
                .transition(DrawableTransitionOptions.withCrossFade())
                .override(root.width, root.height)
                .into(fileImage)
        }
    }

    /* private fun SceytMessageImageItemBinding.updateUploadState(data: FileLoadData) {
         loadProgress.isVisible = data.loading
         if (data.loading)
             loadProgress.setProgress(data.progressPercent)
     }*/


    private fun SceytMessageImageItemBinding.updateUploadState(data: FileLoadData) {
        loadProgress.isVisible = data.loading
        if (data.loading)
            loadProgress.setProgress(data.progressPercent)
    }

    override fun updateUploadingState(data: FileLoadData) {
        binding.updateUploadState(data)
    }

    override fun updateDownloadingState(data: FileLoadData, file: File?) {
        binding.updateDownloadState(data, file)
    }

    private fun SceytMessageImageItemBinding.setupStyle() {
        loadProgress.setProgressColor(context.getCompatColor(MessagesStyle.mediaLoaderColor))
    }
}