package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Log
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.data.models.messages.FileLoadData
import com.sceyt.sceytchatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.persistence.filetransfer.ProgressState.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import java.io.File


class MessageImageViewHolder(
        private val binding: SceytMessageImageItemBinding,
        private val messageListeners: MessageClickListenersImpl?) : BaseFileViewHolder(binding.root) {

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

    override fun bind(item: FileListItem) {
        super.bind(item)

        val transferData = item.file.transferData

        //fileItem.fileLoadData.position = bindingAdapterPosition

        /*setUploadListenerIfNeeded(item)
        downloadIfNeeded(item)*/

        Log.i("sdfsdf", transferData.state.toString())
        when (transferData.state) {
            Pending, Downloading, Uploading -> {
                binding.loadProgress.isVisible = true
                binding.loadProgress.setProgress(transferData.progressPercent)
            }
            Uploaded, Downloaded -> {
                binding.loadProgress.isVisible = false
                Glide.with(itemView.context)
                    .load(item.file.url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .override(binding.root.width, binding.root.height)
                    .into(binding.fileImage)
            }
            Error -> TODO()
        }

        //binding.fileImage.setImageBitmap(null)
        binding.loadProgress.setProgress((item.file.transferData?.progressPercent ?: 0.02f) * 100)
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