package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.content.res.ColorStateList
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelFileBinding
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class FileViewHolder(private val binding: SceytItemChannelFileBinding,
                     private val clickListeners: AttachmentClickListenersImpl,
                     private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) :
        BaseFileViewHolder<ChannelFileItem>(binding.root, needMediaDataCallback) {

    init {
        binding.setupStyle()
        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: ChannelFileItem) {
        super.bind(item)
        val file = (item as? ChannelFileItem.File)?.file ?: return
        setListener()

        with(binding) {
            tvFileName.text = file.name
            tvFileSizeAndDate.text = file.fileSize.toPrettySize()
        }

        viewHolderHelper.transferData?.let {
            updateState(it)
            if (it.filePath.isNullOrBlank() && it.state != TransferState.PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }
    }

    private fun updateState(data: TransferData) {
        if (!viewHolderHelper.updateTransferData(data, fileItem)) return

        /* when (data.state) {
             TransferState.PendingUpload, TransferState.PauseUpload -> {
                 binding.icFile.setImageResource(0)
             }
             TransferState.PendingDownload -> {
                 needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
             }
             TransferState.Downloading, TransferState.Uploading -> {
                 binding.icFile.setImageResource(0)
             }
             TransferState.Uploaded, TransferState.Downloaded -> {
                 binding.icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
             }
             TransferState.ErrorUpload, TransferState.ErrorDownload, TransferState.PauseDownload -> {
                 binding.icFile.setImageResource(0)
             }
             TransferState.FilePathChanged, TransferState.ThumbLoaded -> return
         }*/
    }

    private fun setListener() {
        FileTransferHelper.onTransferUpdatedLiveData.observe(context.asComponentActivity(), ::updateState)
    }

    private fun SceytItemChannelFileBinding.setupStyle() {
        icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
        icFile.backgroundTintList = ColorStateList.valueOf(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}