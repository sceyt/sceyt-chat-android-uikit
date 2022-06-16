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

    override fun bind(item: FileListItem) {
        downloadIfNeeded(item)
        uploadIfNeeded(item)
    }

    open fun updateUploadingState(load: FileLoadData, finish: Boolean) {}
    open fun updateDownloadingState(load: FileLoadData) {}
    open fun downloadFinish(load: FileLoadData, file: File?) {}

    private fun uploadIfNeeded(item: FileListItem) {
        val message = item.sceytMessage
        if (!message.incoming && message.deliveryStatus == DeliveryStatus.Pending && item.fileLoadData.progressPercent != 100) {
            item.setUploadListener {
                runOnMainThread {
                    updateUploadingState(it, false)
                }
            }
        } else updateUploadingState(FileLoadData().loadedState(), true)
    }

    private fun downloadIfNeeded(item: FileListItem) {
        val attachment = item.file ?: return

        val loadedFile = File(itemView.context.filesDir, attachment.name)
        val file = attachment.getLocaleFileByNameOrMetadata(loadedFile)

        if (file != null) {
            downloadFinish(FileLoadData().loadedState(), file)
        } else {
            if (item.fileLoadData.loading) {
                updateDownloadingState(item.fileLoadData)
                return
            }
            loadedFile.deleteOnExit()
            loadedFile.createNewFile()
            item.updateDownloadState(1, loading = true, success = false)
            updateDownloadingState(item.fileLoadData)

            itemView.context.asAppCompatActivity().lifecycleScope.launch(Dispatchers.IO) {
                Ion.with(itemView.context)
                    .load(attachment.url)
                    .progress { downloaded, total ->
                        val progress = (((downloaded.toDouble() / total.toDouble()))) * 100
                        item.updateDownloadState(progress.toInt(), loading = true, success = false)
                        runOnMainThread {
                            updateDownloadingState(item.fileLoadData)
                        }
                    }
                    .write(loadedFile)
                    .setCallback { e, result ->
                        if (result == null && e != null) {
                            item.updateDownloadState(100, loading = false, success = false)
                            runOnMainThread {
                                downloadFinish(item.fileLoadData, result)
                            }
                            loadedFile.delete()
                        } else {
                            item.updateDownloadState(null, loading = false, success = true)
                            runOnMainThread {
                                downloadFinish(item.fileLoadData, result)
                            }
                        }
                    }
            }
        }
    }
}