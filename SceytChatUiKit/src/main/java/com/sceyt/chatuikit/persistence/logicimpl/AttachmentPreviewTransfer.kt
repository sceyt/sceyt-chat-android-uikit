package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.takeIfNotBlank
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentDb
import com.sceyt.chatuikit.persistence.extensions.runCatchingCancellable
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.previewUrl
import com.sceyt.chatuikit.persistence.mappers.toAttachment
import com.sceyt.chatuikit.persistence.mappers.toAttachmentDb
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal fun previewUploadOperationId(messageTid: Long) = "upload:$messageTid:preview"

internal fun previewDownloadOperationId(messageTid: Long) = "download:$messageTid:preview"

internal sealed interface PreviewUploadResult {
    data class Uploaded(val preview: SceytAttachment) : PreviewUploadResult
    data object Unavailable : PreviewUploadResult
    data class Failed(val error: Throwable) : PreviewUploadResult
}

internal class AttachmentPreviewTransfer(
    private val context: Context,
    private val attachmentDao: AttachmentDao,
    private val messagesCache: MessagesCache,
    attachmentLogic: PersistenceAttachmentLogic,
) {
    private val router = AttachmentTransferRouter(context, attachmentLogic)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloads = ConcurrentHashMap<String, Deferred<String?>>()

    suspend fun getPreview(video: SceytAttachment): SceytAttachment? = withContext(Dispatchers.IO) {
        val previewUrl = video.previewUrl() ?: return@withContext null
        attachmentDao.getAttachmentByMsgTidAndUrl(video.messageTid, previewUrl)
            ?.takeIf { it.type == AttachmentTypeEnum.Preview.value }
            ?.let { AttachmentDb(it, null, null).toAttachment() }
    }

    suspend fun upload(video: SceytAttachment): PreviewUploadResult {
        val sourcePath = video.originalFilePath.takeIfNotBlank() ?: video.filePath.takeIfNotBlank()
            ?: return PreviewUploadResult.Unavailable
        val file = withContext(Dispatchers.IO) {
            FileResizeUtil.getVideoThumbAsFile(context, sourcePath, PREVIEW_MAX_SIZE)
        }.getOrElse {
            SceytLog.w(TAG, "Couldn't create a preview for messageTid: ${video.messageTid}", it)
            return PreviewUploadResult.Unavailable
        }

        val preview = SceytAttachment(
            id = null,
            messageId = video.messageId,
            messageTid = video.messageTid,
            userId = video.userId,
            name = "${video.name.substringBeforeLast('.')}_preview.jpg",
            type = AttachmentTypeEnum.Preview.value,
            metadata = null,
            fileSize = file.length(),
            createdAt = System.currentTimeMillis(),
            url = null,
            filePath = file.path,
            transferState = null,
            progressPercent = null,
            originalFilePath = null,
            linkPreviewDetails = null,
        )

        return runCatchingCancellable { uploadPreview(preview) }.fold(
            onSuccess = { PreviewUploadResult.Uploaded(it) },
            onFailure = { PreviewUploadResult.Failed(it) },
        )
    }

    fun cancelAll() {
        scope.coroutineContext.cancelChildren()
        downloads.clear()
    }

    private suspend fun uploadPreview(preview: SceytAttachment): SceytAttachment {
        val executor = router.upload(
            attachment = preview,
            operationId = previewUploadOperationId(preview.messageTid),
            isSharedUpload = false,
            onUploadPrepared = { prepared, _ -> prepared },
        )
        return when (val result = executor.execute(noProgress)) {
            is FileTransferResult.Success -> {
                val uploaded = (executor.attachment ?: preview).copy(url = result.result)
                check(save(uploaded)) { "Video attachment not found" }
                uploaded
            }

            is FileTransferResult.Failure -> throw result.error
            is FileTransferResult.Interrupted -> throw result.error
        }
    }

    fun loadPreviewPath(video: SceytAttachment, onReady: (String) -> Unit) {
        scope.launch {
            runCatchingCancellable { getPreviewPath(video) }
                .onFailure { SceytLog.w(TAG, "Couldn't load preview for messageTid: ${video.messageTid}", it) }
                .getOrNull()
                ?.let(onReady)
        }
    }

    private suspend fun getPreviewPath(video: SceytAttachment): String? {
        val preview = getPreview(video) ?: return null
        val url = preview.url.takeIfNotBlank() ?: return null
        preview.filePath?.takeIf { File(it).isFile }?.let { return it }

        val download = downloads.computeIfAbsent(url) {
            scope.async { download(preview.copy(url = url)) }
        }
        download.invokeOnCompletion { downloads.remove(url, download) }
        return download.await()
    }

    private suspend fun download(preview: SceytAttachment): String? {
        val destination = SceytChatUIKit.fileTransfer.destinationProvider
            .provideDestination(context, preview)
        val executor = router.download(
            attachment = preview,
            operationId = previewDownloadOperationId(preview.messageTid),
            destination = destination
        )
        return when (val result = executor.execute(noProgress)) {
            is FileTransferResult.Success -> {
                val path = result.result
                attachmentDao.updateAttachmentFilePathByMsgTidAndType(
                    msgTid = preview.messageTid,
                    type = AttachmentTypeEnum.Preview.value,
                    filePath = path
                )
                messagesCache.replaceAttachmentByType(preview.copy(filePath = path))
                path
            }

            is FileTransferResult.Failure, is FileTransferResult.Interrupted -> {
                SceytLog.w(TAG, "Couldn't download preview for messageTid: ${preview.messageTid}")
                null
            }
        }
    }

    private suspend fun save(preview: SceytAttachment): Boolean = withContext(Dispatchers.IO) {
        val video = attachmentDao.getAttachmentByMsgTidAndType(
            msgTid = preview.messageTid,
            type = AttachmentTypeEnum.Video.value
        ) ?: return@withContext false

        val entity = preview.toAttachmentDb(
            messageId = video.messageId,
            messageTid = video.messageTid,
            channelId = video.channelId,
            viewOnce = video.viewOnce
        ).attachmentEntity
        attachmentDao.replaceAttachmentByType(entity)
        messagesCache.replaceAttachmentByType(preview)
        true
    }

    private companion object {
        const val TAG = "AttachmentPreviewTransfer"
        const val PREVIEW_MAX_SIZE = 800f
        val noProgress = FileTransferCallback { }
    }
}
