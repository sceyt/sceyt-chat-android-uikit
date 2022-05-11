package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.ui.data.models.messages.UploadData
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.utils.FileCompressorUtil
import java.io.File

abstract class BaseFileViewHolder(itemView: View)
    : RecyclerView.ViewHolder(itemView) {
    abstract fun bindTo(item: FileListItem)

    protected fun downloadWithUrl(attachment: Attachment) {
        val tmpFile = File(itemView.context.externalCacheDir, attachment.name)
        if (tmpFile.exists() && FileCompressorUtil.getFileSize(tmpFile.path) == attachment.uploadedFileSize) {
            postDownloadingValue(1f, attachment.url)
        } else {
            tmpFile.deleteOnExit()
            //  MessagesAdapter.updateFileLoadData(attachment.name, isLoading = true, 0.0)
            tmpFile.createNewFile()

            /* Ion.with(itemView.context)
                 .load(attachment.url)
                 .progress { downloaded, total ->
                     val progress = (((downloaded.toDouble() / total.toDouble())))
                     MessagesAdapter.updateFileLoadData(attachment.name, isLoading = true, progress = progress)
                     postDownloadingValue(progress.toFloat(), attachment.url)
                 }
                 .write(tmpFile)
                 .setCallback { e, result ->
                     if (result == null && e != null) {
                         MessagesAdapter.updateFileLoadData(attachment.name, false, 1.0)
                         tmpFile.delete()
                         postDownloadingValue(1f, attachment.url)
                     } else {
                         MessagesAdapter.updateFileLoadData(attachment.name, false, 0.0)
                     }
                 }*/
        }
    }

    private fun postDownloadingValue(progress: Float, url: String) {
        /*  val value = uploadMutableLiveData?.value.let {
              it?.apply {
                  this[url] = UploadData(progress, null)
              } ?: LinkedHashMap<String, UploadData>().apply {
                  this[url] = UploadData(progress, null)
              }
          }
          uploadMutableLiveData?.postValue(value)*/
    }
}