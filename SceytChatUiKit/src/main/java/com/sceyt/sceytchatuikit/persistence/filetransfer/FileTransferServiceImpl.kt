package com.sceyt.sceytchatuikit.persistence.filetransfer

import android.content.Context
import androidx.work.WorkManager
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.sceytchatuikit.persistence.logics.filetransferlogic.FileTransferLogic
import com.sceyt.sceytchatuikit.persistence.workers.SendAttachmentWorkManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

internal class FileTransferServiceImpl(private var context: Context,
                                       private var fileTransferLogic: FileTransferLogic) : FileTransferService {
    private var tasksMap = ConcurrentHashMap<String, TransferTask>()

    private var listeners: FileTransferListeners.Listeners = object : FileTransferListeners.Listeners {
        override fun upload(attachment: SceytAttachment,
                            transferTask: TransferTask) {
            fileTransferLogic.uploadFile(attachment, transferTask)
        }

        override fun uploadSharedFile(attachment: SceytAttachment, transferTask: TransferTask) {
            fileTransferLogic.uploadSharedFile(attachment, transferTask)
        }

        override fun download(attachment: SceytAttachment, transferTask: TransferTask) {
            fileTransferLogic.downloadFile(attachment, transferTask)
        }

        override fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
            fileTransferLogic.pauseLoad(attachment, state)
        }

        override fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
            fileTransferLogic.resumeLoad(attachment, state)
        }

        override fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData) {
            fileTransferLogic.getAttachmentThumb(messageTid, attachment, thumbData)
        }
    }


    override fun upload(attachment: SceytAttachment,
                        transferTask: TransferTask) {
        tasksMap[attachment.messageTid.toString()] = transferTask
        listeners.upload(attachment, transferTask)
    }

    override fun uploadSharedFile(attachment: SceytAttachment, transferTask: TransferTask) {
        tasksMap[attachment.messageTid.toString()] = transferTask
        listeners.uploadSharedFile(attachment, transferTask)
    }

    override fun download(attachment: SceytAttachment, transferTask: TransferTask) {
        tasksMap[attachment.messageTid.toString()] = transferTask
        listeners.download(attachment, transferTask)
    }

    override fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
        listeners.pause(messageTid, attachment, state)
    }

    override fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
        val workInfo = WorkManager.getInstance(context).getWorkInfosByTag(messageTid.toString())
        if ((state == PauseUpload || state == ErrorUpload) && (workInfo.get().isEmpty() || workInfo.isCancelled))
            SendAttachmentWorkManager.schedule(context, messageTid, null)
        else
            listeners.resume(messageTid, attachment, state)
    }

    override fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData) {
        listeners.getThumb(messageTid, attachment, thumbData)
    }

    override fun setCustomListener(fileTransferListeners: FileTransferListeners.Listeners) {
        listeners = fileTransferListeners
    }

    override fun findOrCreateTransferTask(attachment: SceytAttachment): TransferTask {
        val isUploading: Boolean = when (attachment.transferState) {
            PendingDownload, Downloading, Downloaded, PauseDownload, ErrorDownload -> false
            else -> true
        }
        return tasksMap[attachment.messageTid.toString()] ?: run {
            FileTransferHelper.createTransferTask(attachment, isUploading)
        }
    }

    override fun findTransferTask(attachment: SceytAttachment): TransferTask? {
        return tasksMap[attachment.messageTid.toString()]
    }

    override fun getTasks() = tasksMap

    override fun clearPreparingThumbPaths() {
        fileTransferLogic.clearPreparingThumbPaths()
    }
}

fun interface TransferResultCallback {
    fun onResult(sceytResponse: SceytResponse<String>)
}

fun interface ProgressUpdateCallback {
    fun onProgress(date: TransferData)
}

fun interface UpdateFileLocationCallback {
    fun onUpdateFileLocation(path: String)
}

fun interface ThumbCallback {
    fun onThumb(path: String, thumbData: ThumbData)
}

