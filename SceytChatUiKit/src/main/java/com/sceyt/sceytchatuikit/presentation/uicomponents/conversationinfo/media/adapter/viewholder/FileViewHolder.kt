package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.FileLoadData
import com.sceyt.sceytchatuikit.databinding.ItemChannelFileBinding
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import java.io.File
import java.util.*

class FileViewHolder(private val binding: ItemChannelFileBinding,
                     private val clickListeners: AttachmentClickListenersImpl) : BaseFileViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        val file = (item as? FileListItem.File)?.file ?: return

        with(binding) {
            tvFileName.text = file.name

            val sizeText = if (item.message.incoming) {
                file.fileSize.toPrettySize()
            } else {
                val size = if (file.fileSize == 0L) {
                    getFileSize(file.url)
                } else file.fileSize

                size.toPrettySize()
            }

            val date = DateTimeUtil.convertDateToString(Date(item.message.createdAt), "dd/MM/yyyy")
            val sizeAndDate = "$sizeText, $date"
            tvFileSizeAndDate.text = sizeAndDate
        }
    }

    private fun ItemChannelFileBinding.updateLoadState(data: FileLoadData) {
        loadProgress.isVisible = data.loading
        icFile.setImageResource(if (data.loading) 0 else R.drawable.sceyt_ic_file)
        loadProgress.progress = data.progressPercent.toInt()
    }

    override fun updateUploadingState(data: FileLoadData) {
        binding.updateLoadState(data)
    }

    override fun updateDownloadingState(data: FileLoadData, file: File?) {
        binding.updateLoadState(data)
    }
}