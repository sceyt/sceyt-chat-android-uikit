package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.koushikdutta.ion.Ion
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.ui.data.models.messages.FileLoadData
import com.sceyt.chat.ui.extensions.asAppCompatActivity
import com.sceyt.chat.ui.extensions.runOnMainThread
import com.sceyt.chat.ui.presentation.common.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.getLocaleFileByNameOrMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


abstract class BaseFileViewHolder(itemView: View) : BaseViewHolder<FileListItem>(itemView) {
    protected lateinit var fileItem: FileListItem

    override fun bind(item: FileListItem) {
        fileItem = item
        setUploadListenerIfNeeded(item)
        downloadIfNeeded(item)
    }

    open fun updateUploadingState(data: FileLoadData) {}
    open fun updateDownloadingState(data: FileLoadData, file: File? = null) {}

    private fun setUploadListenerIfNeeded(item: FileListItem) {
        val message = item.sceytMessage
        if (message.deliveryStatus == DeliveryStatus.Pending && item.fileLoadData.progressPercent != 100f) {
            updateUploadingState(item.fileLoadData.apply { loading = true })
            item.setUploadListener { loadData ->
                if (checkLoadDataIsForCurrent(data = loadData))
                    runOnMainThread { updateUploadingState(loadData) }
            }
        }
    }

    private fun downloadIfNeeded(item: FileListItem) {
        val attachment = item.file

        val loadedFile = File(itemView.context.filesDir, attachment.name)
        val file = attachment.getLocaleFileByNameOrMetadata(loadedFile)

        if (file != null) {
            updateDownloadingState(item.fileLoadData, file)
            item.setDownloadProgressListener(null)
        } else {

            item.setDownloadProgressListener { loadData, outFile ->
                if (checkLoadDataIsForCurrent(data = loadData))
                    runOnMainThread { updateDownloadingState(loadData, outFile) }
            }

            if (item.fileLoadData.loading) {
                updateDownloadingState(item.fileLoadData)
                return
            }

            loadedFile.deleteOnExit()
            loadedFile.createNewFile()
            item.updateDownloadState(1f, loading = true)

            itemView.context.asAppCompatActivity().lifecycleScope.launch(Dispatchers.IO) {
                Ion.with(itemView.context)
                    .load(attachment.url)
                    .progress { downloaded, total ->
                        val progress = ((downloaded / total.toFloat())) * 100f
                        item.updateDownloadState(progress, loading = true)
                    }
                    .write(loadedFile)
                    .setCallback { e, result ->
                        if (result == null && e != null) {
                            loadedFile.delete()
                            item.downloadFinish(result, false)
                        } else {
                            item.downloadFinish(result, true)
                        }
                    }
            }
        }
    }

    private fun checkLoadDataIsForCurrent(data: FileLoadData): Boolean {
        return (::fileItem.isInitialized && fileItem.fileLoadData.loadId == data.loadId)
    }
}