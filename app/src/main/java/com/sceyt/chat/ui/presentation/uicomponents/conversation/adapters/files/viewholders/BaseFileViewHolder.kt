package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import android.view.View
import com.koushikdutta.ion.Ion
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.getLocaleFileByNameOrMetadata
import java.io.File


abstract class BaseFileViewHolder(itemView: View) : BaseViewHolder<FileListItem>(itemView) {

    protected fun setUploadListenerIfNeeded(item: FileListItem, finishCb: ((success: Boolean) -> Unit)? = null) {
        val message = item.sceytMessage
        if (!message.incoming && message.status == DeliveryStatus.Pending && item.fileLoadData.progressPercent != 100)
            item.setUploadListener(finishCb)
        else finishCb?.invoke(true)
    }

    protected fun downloadIfNeeded(item: FileListItem, finishCb: ((File) -> Unit)? = null) {
        val attachment = item.file ?: return

        item.downloadSuccess = finishCb
        val loadedFile = File(itemView.context.filesDir, attachment.name)
        val file = attachment.getLocaleFileByNameOrMetadata(loadedFile)

        if (file != null) {
            item.downloadSuccess?.invoke(file)
        } else {
            if (item.fileLoadData.loading)
                return
            loadedFile.deleteOnExit()
            loadedFile.createNewFile()
            item.updateDownloadState(1, true)

            Ion.with(itemView.context)
                .load(attachment.url)
                .progress { downloaded, total ->
                    val progress = (((downloaded.toDouble() / total.toDouble()))) * 100
                    item.updateDownloadState(progress.toInt(), true)
                }
                .write(loadedFile)
                .setCallback { e, result ->
                    if (result == null && e != null) {
                        item.updateDownloadState(100, false)
                        loadedFile.delete()
                    } else {
                        item.updateDownloadState(null, false)
                        item.downloadSuccess?.invoke(result)
                    }
                }
        }
    }
}