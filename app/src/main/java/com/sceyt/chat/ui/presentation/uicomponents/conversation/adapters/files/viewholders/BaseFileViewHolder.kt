package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.ui.BuildConfig
import com.sceyt.chat.ui.data.models.messages.AttachmentMetadata
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.utils.FileCompressorUtil
import java.io.File


abstract class BaseFileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    abstract fun bindTo(item: FileListItem)

    private fun getFileFromMetadata(item: FileListItem): File? {
        val metadata = item.file?.metadata ?: return null
        try {
            val data = Gson().fromJson(metadata, AttachmentMetadata::class.java)
            return File(data.localPath)
        } catch (e: Exception) {
        }
        return null
    }

    protected fun setUploadListenerIfNeeded(item: FileListItem) {
        val message = item.sceytUiMessage
        if (!message.incoming && message.status == DeliveryStatus.Pending)
            item.setUploadListener(item.file)
    }

    protected fun downloadIfNeeded(item: FileListItem, downloadResult: ((File?, Exception?) -> Unit)? = null) {
        val message = item.sceytUiMessage
        val fileFromMetadata = getFileFromMetadata(item)

        if (!message.incoming && message.status == DeliveryStatus.Pending) {
            if (fileFromMetadata != null && fileFromMetadata.exists()) {
                downloadResult?.invoke(fileFromMetadata, null)
                return
            }
        }

        val attachment = item.file ?: return

        val tmpFile = File(itemView.context.externalCacheDir, attachment.name)

        if (tmpFile.exists() && FileCompressorUtil.getFileSize(tmpFile.path) == attachment.uploadedFileSize) {
            downloadResult?.invoke(tmpFile, null)
        } else {
            tmpFile.deleteOnExit()

            if (fileFromMetadata != null && fileFromMetadata.exists()) {
                downloadResult?.invoke(fileFromMetadata, null)
                return
            }

            tmpFile.createNewFile()
            item.updateDownloadState(1, true)

            Ion.with(itemView.context)
                .load(attachment.url)
                .progress { downloaded, total ->
                    val progress = (((downloaded.toDouble() / total.toDouble()))) * 100
                    item.updateDownloadState(progress.toInt(), true)
                }
                .write(tmpFile)
                .setCallback { e, result ->
                    if (result == null && e != null) {
                        item.updateDownloadState(100, false)
                        tmpFile.delete()
                    } else
                        item.updateDownloadState(null, false)
                    downloadResult?.invoke(result, e)
                }
        }
    }

    protected fun openFile(item: FileListItem, context: Context) {
        if (item.fileLoadData.loading) return
        val fileName = item.file?.name
        var uri: Uri? = null
        if (fileName != null) {
            val tmpF = File(itemView.context.externalCacheDir, fileName)
            if (tmpF.exists()) {
                uri = FileProvider.getUriForFile(itemView.context,
                    BuildConfig.APPLICATION_ID + ".provider", tmpF)
            } else {
                val fileFromMetadata = getFileFromMetadata(item)
                if (fileFromMetadata != null && fileFromMetadata.exists())
                    uri = Uri.fromFile(fileFromMetadata)
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
}