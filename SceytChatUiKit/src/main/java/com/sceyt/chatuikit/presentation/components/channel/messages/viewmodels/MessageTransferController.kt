package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.file_transfer.isCompleted
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns attachment transfer/progress handling that used to live inside [MessageListViewModel]:
 * applying [TransferData] updates onto rendered message attachments, requesting downloads/thumbs and
 * pausing/resuming uploads. Transfers that arrive while the list isn't visible are parked via
 * [deferUpdate] and applied by [flushDeferred] when the binding resumes.
 */
internal class MessageTransferController(
    private val scope: CoroutineScope,
    private val defaultDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val fileTransferService: FileTransferService,
    private val pauseOrResumeTransferUseCase: PauseOrResumeTransferUseCase,
    private val store: MessageListStore,
    private val channelId: () -> Long,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // Transfers that arrived while the list wasn't visible, keyed by messageTid; flushed on resume.
    private val deferredUpdates = hashMapOf<Long, TransferData>()

    /** Parks a transfer update to be applied once the list is visible again. */
    fun deferUpdate(transfer: TransferData) {
        deferredUpdates[transfer.messageTid] = transfer
    }

    /** Applies and clears all parked transfer updates. No-op when none are parked. */
    suspend fun flushDeferred() {
        if (deferredUpdates.isEmpty()) return
        val parked = deferredUpdates.values.toList()
        deferredUpdates.clear()
        parked.forEach { updateProgress(it, updateRecyclerView = true) }
    }

    suspend fun updateProgress(
        data: TransferData,
        updateRecyclerView: Boolean,
    ) = withContext(defaultDispatcher) {
        val messages = ArrayList(store.items)
        messages.findIndexed { item ->
            item is MessageItem && item.message.tid == data.messageTid
        }?.let { (_, item) ->
            val message = (item as? MessageItem)?.message ?: return@withContext
            val attachments = message.attachments?.toMutableList() ?: return@withContext

            val predicate: (SceytAttachment) -> Boolean = when (data.state) {
                Uploading, PendingUpload, PauseUpload, Uploaded, Preparing, WaitingToUpload -> { attachment ->
                    attachment.messageTid == data.messageTid
                }

                else -> { attachment ->
                    attachment.url == data.url
                }
            }
            val foundAttachmentFile = item.message.files?.find { listItem ->
                predicate(listItem.attachment)
            }

            if (data.state == ThumbLoaded) {
                if (data.thumbData?.key == ThumbFor.MessagesLisView.value) {
                    foundAttachmentFile?.updateThumbPath(data.filePath)
                }
                return@withContext
            } else {
                for ((attachmentIndex, sceytAttachment) in attachments.withIndex()) {
                    if (predicate(sceytAttachment)) {
                        val attachmentWithTransfer = sceytAttachment.getUpdatedWithTransferData(
                            data = data
                        )
                        val updatedAttachment = foundAttachmentFile?.updateAttachment(
                            file = attachmentWithTransfer
                        )
                        attachments[attachmentIndex] = updatedAttachment ?: attachmentWithTransfer
                        val updatedItem = item.copy(
                            message = message.copy(attachments = attachments)
                        )
                        withContext(mainDispatcher) {
                            store.updateItem(
                                predicate = { it.message.tid == data.messageTid },
                                diff = MessageDiff.DEFAULT_FALSE.copy(filesChanged = true),
                                notifyVisibleOnly = !updateRecyclerView,
                                update = { updatedItem }
                            )
                        }
                        break
                    }
                }
            }
        }

        if (data.state == Downloaded) {
            messages.forEach { item ->
                if (item is MessageItem && item.message.parentMessage?.tid == data.messageTid) {
                    val message = item.message
                    val updatedItem = item.copy(
                        message = message.copy(
                            parentMessage = message.parentMessage.copy(
                                attachments = item.message.parentMessage.attachments?.map { attachment ->
                                    if (attachment.url == data.url) {
                                        attachment.copy(filePath = data.filePath)
                                    } else attachment
                                }
                            )))

                    withContext(mainDispatcher) {
                        store.updateItem(
                            predicate = { it.message.tid == message.tid },
                            diff = MessageDiff.DEFAULT_FALSE.copy(replyContainerChanged = true),
                            update = { updatedItem }
                        )
                    }
                }
            }
        }
    }

    fun needMediaInfo(data: NeedMediaInfoData) {
        val attachment = data.item
        when (data) {
            is NeedMediaInfoData.NeedDownload -> {
                scope.launch(ioDispatcher) {
                    fileTransferService.download(
                        attachment = attachment,
                        transferTask = fileTransferService.findOrCreateTransferTask(attachment)
                    )
                }
            }

            is NeedMediaInfoData.NeedThumb -> {
                scope.launch(ioDispatcher) {
                    fileTransferService.getThumb(attachment.messageTid, attachment, data.thumbData)
                }
            }
        }
    }

    fun shouldDeferTransferUpdate(transfer: TransferData): Boolean {
        return transfer.state.isCompleted() ||
                transfer.state == FilePathChanged || isMessageListThumbLoaded(transfer)
    }

    fun isMessageListThumbLoaded(transfer: TransferData): Boolean {
        return transfer.state == ThumbLoaded && transfer.thumbData?.key == ThumbFor.MessagesLisView.value
    }

    fun clearPreparingThumbs() {
        fileTransferService.clearPreparingThumbPaths()
    }

    fun pauseOrResumeUpload(item: FileListItem) {
        scope.launch {
            pauseOrResumeTransferUseCase(item.attachment, channelId())
        }
    }
}