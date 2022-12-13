package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.view.View
import com.sceyt.sceytchatuikit.data.models.messages.FileLoadData
import com.sceyt.sceytchatuikit.extensions.isNull
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import java.io.File


abstract class BaseFileViewHolder(itemView: View) : BaseViewHolder<FileListItem>(itemView) {
    protected lateinit var fileItem: FileListItem
    val isFileItemInitialized get() = this::fileItem.isInitialized
    protected val context: Context by lazy { itemView.context }
    protected var listenerKey: String = ""

    override fun bind(item: FileListItem) {
        fileItem = item

        //fileItem.fileLoadData.position = bindingAdapterPosition

        /*setUploadListenerIfNeeded(item)
        downloadIfNeeded(item)*/
    }

    open fun updateUploadingState(data: FileLoadData) {}
    open fun updateDownloadingState(data: FileLoadData, file: File? = null) {}

    protected fun getKey(): String {
        if (isFileItemInitialized.not()) return ""
        val data = fileItem.file
        val key: String = if (data.tid.isNull() || data.tid == 0L) {
            data.url.toString()
        } else {
            data.tid.toString()
        }
        return key
    }

    /* private fun setUploadListenerIfNeeded(item: FileListItem) {
         val message = item.sceytMessage
         if (message.deliveryStatus == DeliveryStatus.Pending && item.fileLoadData.progressPercent != 100f) {
             updateUploadingState(item.fileLoadData.apply { loading = true })
             item.setUploadListener { loadData ->
                 if (checkLoadDataIsForCurrent(data = loadData))
                     runOnMainThread { updateUploadingState(loadData) }
             }
         }
     }
 */
    /*  private fun downloadIfNeeded(item: FileListItem) {
          val attachment = item.file


          val loadedFile = File(itemView.context.filesDir, attachment.name)
          val file = attachment.getLocaleFileByNameOrMetadata(loadedFile)

          if (file != null) {
              val loadData = if (item.sceytMessage.deliveryStatus == DeliveryStatus.Pending)
                  item.fileLoadData else item.fileLoadData.loadedState()
              updateDownloadingState(loadData, file)
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

              itemView.context.asComponentActivity().lifecycleScope.launch(Dispatchers.IO) {
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
                              item.downloadFinish(null, false)
                          } else {
                              item.downloadFinish(result, true)
                          }
                      }
              }
          }
      }*/
/*
    private fun checkLoadDataIsForCurrent(data: FileLoadData): Boolean {
        return (::fileItem.isInitialized && fileItem.fileLoadData.loadId == data.loadId
                && fileItem.fileLoadData.position == data.position)
    }*/
}