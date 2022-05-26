package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.ui.BuildConfig
import com.sceyt.chat.ui.data.models.messages.AttachmentMetadata
import com.sceyt.chat.ui.extensions.getFileSize
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import java.io.File


abstract class BaseFileViewHolder(itemView: View) : BaseViewHolder<FileListItem>(itemView) {

    protected fun setUploadListenerIfNeeded(item: FileListItem, finishCb: ((success: Boolean) -> Unit)? = null) {
        val message = item.sceytUiMessage
        if (!message.incoming && message.status == DeliveryStatus.Pending)
            item.setUploadListener(finishCb)
        else finishCb?.invoke(true)
    }

    protected fun downloadIfNeeded(item: FileListItem) {
        val attachment = item.file ?: return
        val fileFromMetadata = getFileFromMetadata(item)

        if (fileFromMetadata != null && fileFromMetadata.exists()) {
            item.downloadSuccess?.invoke(fileFromMetadata)
            return
        }

        val loadedFile = File(itemView.context.filesDir, attachment.name)

        if (loadedFile.exists() && getFileSize(loadedFile.path) == attachment.uploadedFileSize) {
            item.downloadSuccess?.invoke(loadedFile)
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

    protected fun openFile(item: FileListItem, context: Context) {
        val fileName = item.file?.name
        var uri: Uri? = null
        if (fileName != null) {
            val loadedFile = File(itemView.context.filesDir, fileName)
            if (loadedFile.exists()) {
                uri = FileProvider.getUriForFile(itemView.context,
                    BuildConfig.APPLICATION_ID + ".provider", loadedFile)
            } else {
                getFileFromMetadata(item)?.let {
                    uri = FileProvider.getUriForFile(itemView.context,
                        BuildConfig.APPLICATION_ID + ".provider", it)
                }
            }
        }

        if (uri != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                    .setData(uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "You may not have a proper app for viewing this content", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileFromMetadata(item: FileListItem): File? {
        val metadata = item.file?.metadata ?: return null
        try {
            val data = Gson().fromJson(metadata, AttachmentMetadata::class.java)
            return File(data.localPath)
        } catch (e: Exception) {
        }
        return null
    }
}