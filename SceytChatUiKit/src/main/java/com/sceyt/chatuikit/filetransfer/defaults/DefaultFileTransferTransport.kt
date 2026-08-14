package com.sceyt.chatuikit.filetransfer.defaults

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import java.util.concurrent.ConcurrentHashMap

class DefaultFileTransferTransport : FileTransferTransport {
    private val transferUtility = FileTransferUtility()

    private val uploadAttachments = ConcurrentHashMap<String, SceytAttachment>()
    private val downloadAttachments = ConcurrentHashMap<String, SceytAttachment>()

    override fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback<String>,
    ) {
        val attachment = request.attachment.copy(
            filePath = request.sourceFile.path,
        )
        uploadAttachments[request.operationId] = attachment

        transferUtility.uploadFile(
            attachment = attachment,
            onProgress = callback::onProgress,
            onResult = { response ->
                uploadAttachments.remove(request.operationId)

                when (response) {
                    is SceytResponse.Success -> {
                        callback.onSuccess(response.data)
                    }

                    is SceytResponse.Error -> {
                        callback.onFailure(response.exception)
                    }
                }
            },
        )
    }

    override fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback<String>,
    ) {
        val attachment = request.attachment.copy(url = request.url)
        downloadAttachments[request.operationId] = attachment

        transferUtility.downloadFile(
            attachment = attachment,
            destFile = request.destinationFile,
            onProgress = callback::onProgress,
            onResult = { response ->
                downloadAttachments.remove(request.operationId)

                when (response) {
                    is SceytResponse.Success -> {
                        callback.onSuccess(response.data)
                    }

                    is SceytResponse.Error -> {
                        callback.onFailure(response.exception)
                    }
                }
            },
        )
    }

    override fun pause(operationId: String): Boolean {
        uploadAttachments[operationId]?.let { attachment ->
            transferUtility.pauseUpload(attachment)
            return true
        }

        downloadAttachments[operationId]?.let { attachment ->
            transferUtility.pauseDownload(attachment)
            return true
        }

        return false
    }

    override fun resume(operationId: String): Boolean {
        uploadAttachments[operationId]?.let { attachment ->
            return transferUtility.resumeUpload(attachment)
        }

        downloadAttachments[operationId]?.let { attachment ->
            return transferUtility.resumeDownload(attachment)
        }

        return false
    }
}
