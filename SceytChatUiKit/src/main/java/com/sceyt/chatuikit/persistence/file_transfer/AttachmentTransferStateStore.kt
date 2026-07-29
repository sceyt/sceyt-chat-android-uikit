package com.sceyt.chatuikit.persistence.file_transfer

import android.util.Size
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload

internal object AttachmentTransferStateStore {
    private const val MAX_ENTRY_COUNT = 512
    private val entries = object : LinkedHashMap<TransferKey, Entry>(MAX_ENTRY_COUNT, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<TransferKey, Entry>): Boolean {
            return size > MAX_ENTRY_COUNT
        }
    }

    @Synchronized
    fun put(update: TransferData): TransferData? {
        if (update.state == ThumbLoaded) {
            return putThumb(update)
        }

        val key = resolveKey(update)
        val entry = entries.getOrPut(key) { Entry() }
        val current = entry.transferData
        if (!isValidTransition(current?.state, current?.progressPercent ?: 0f, update)) return null

        val merged = current?.merge(update) ?: update
        entry.transferData = merged
        return merged
    }

    @Synchronized
    fun getTransferData(attachment: SceytAttachment): TransferData? {
        val current = findEntry(attachment)?.transferData ?: return null
        val attachmentState = attachment.transferState
        val attachmentProgress = attachment.progressPercent ?: 0f
        if (!isValidTransition(
                currentState = attachmentState,
                newState = current.state,
                currentProgress = attachmentProgress,
                newProgress = current.progressPercent
            )
        ) {
            return null
        }
        return current.merge(attachment)
    }

    @Synchronized
    fun getThumbPath(attachment: SceytAttachment, thumbFor: ThumbFor, size: Size?): String? {
        val entry = findEntry(attachment) ?: return null
        if (size != null) {
            entry.thumbs[ThumbKey(thumbFor.value, size)]?.let {
                return it
            }
        }
        return entry.thumbs.entries.lastOrNull { it.key.thumbFor == thumbFor.value }?.value
    }

    fun getUpdatedAttachment(
        attachment: SceytAttachment,
        transferData: TransferData
    ): SceytAttachment {
        val merged = transferData.merge(attachment)
        return attachment.copy(
            transferState = merged.state,
            progressPercent = merged.progressPercent,
            filePath = merged.filePath,
            url = merged.url
        )
    }

    fun isTransferDataForAttachment(
        transferData: TransferData,
        attachment: SceytAttachment,
    ): Boolean {
        if (transferData.messageTid != attachment.messageTid) return false
        return attachment.type != AttachmentTypeEnum.Link.value
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }

    private fun putThumb(update: TransferData): TransferData? {
        val thumbData = update.thumbData ?: return null
        val key = resolveKey(update)
        val entry = entries.getOrPut(key) { Entry() }
        val currentFilePath = entry.transferData?.filePath
        if (!currentFilePath.isNullOrBlank() && !thumbData.filePath.isNullOrBlank()
            && currentFilePath != thumbData.filePath
        ) return null

        entry.thumbs[ThumbKey(thumbData.key, thumbData.size)] = update.filePath
        return update
    }

    private fun findEntry(attachment: SceytAttachment): Entry? {
        if (attachment.url.isNotNullOrBlank()) {
            entries[TransferKey(attachment.messageTid, IdentityType.Url, attachment.url)]?.let {
                return it
            }
        }

        if (attachment.filePath.isNotNullOrBlank()) {
            entries[TransferKey(
                messageTid = attachment.messageTid,
                type = IdentityType.FilePath,
                value = attachment.filePath
            )]?.let {
                return it
            }
        }

        if (attachment.originalFilePath.isNotNullOrBlank()) {
            entries[TransferKey(
                messageTid = attachment.messageTid,
                type = IdentityType.FilePath,
                value = attachment.originalFilePath
            )]?.let {
                return it
            }
        }

        entries[TransferKey(attachment.messageTid, IdentityType.Message, null)]?.let {
            return it
        }
        return entries.filterKeys { it.messageTid == attachment.messageTid }
            .takeIf { it.size == 1 }
            ?.values
            ?.singleOrNull()
    }

    private fun resolveKey(update: TransferData): TransferKey {
        val preferred = preferredKey(update)
        if (entries.containsKey(preferred)) return preferred

        val sameMessageEntries = entries.filterKeys { it.messageTid == update.messageTid }
        if (preferred.type != IdentityType.Message) {
            val previousKey = sameMessageEntries.keys.singleOrNull()
            if (previousKey != null &&
                (previousKey.type == IdentityType.Message || update.state == FilePathChanged)
            ) {
                entries.remove(previousKey)?.let { entry ->
                    entries[preferred] = entry
                }
                return preferred
            }
        }

        if (sameMessageEntries.size == 1) return sameMessageEntries.keys.single()
        return preferred
    }

    private fun preferredKey(update: TransferData): TransferKey {
        return when {
            update.state.isUploadState() && update.filePath.isNotNullOrBlank() ->
                TransferKey(update.messageTid, IdentityType.FilePath, update.filePath)

            update.state == FilePathChanged && update.filePath.isNotNullOrBlank() ->
                TransferKey(update.messageTid, IdentityType.FilePath, update.filePath)

            update.url.isNotNullOrBlank() ->
                TransferKey(update.messageTid, IdentityType.Url, update.url)

            else ->
                TransferKey(update.messageTid, IdentityType.Message, null)
        }
    }

    private fun isValidTransition(
        currentState: TransferState?,
        currentProgress: Float,
        update: TransferData
    ): Boolean {
        return isValidTransition(
            currentState = currentState,
            newState = update.state,
            currentProgress = currentProgress,
            newProgress = update.progressPercent
        )
    }

    private fun isValidTransition(
        currentState: TransferState?,
        newState: TransferState,
        currentProgress: Float,
        newProgress: Float,
    ): Boolean {
        if (currentState == newState && newState.isCompleted()) return true
        return TransferStateValidator.isValidStateTransition(
            currentState = currentState,
            newState = newState,
            currentProgress = currentProgress,
            newProgress = newProgress
        )
    }

    private fun TransferData.merge(update: TransferData): TransferData {
        return update.copy(
            filePath = update.filePath.takeUnless { it.isNullOrBlank() } ?: filePath,
            url = update.url.takeUnless { it.isNullOrBlank() } ?: url,
            fileLoadedSize = update.fileLoadedSize ?: fileLoadedSize,
            fileTotalSize = update.fileTotalSize ?: fileTotalSize,
        )
    }

    private fun TransferData.merge(attachment: SceytAttachment): TransferData {
        return copy(
            filePath = filePath.takeUnless { it.isNullOrBlank() } ?: attachment.filePath,
            url = url.takeUnless { it.isNullOrBlank() } ?: attachment.url,
        )
    }

    private fun TransferState.isUploadState(): Boolean {
        return when (this) {
            PendingUpload, Uploading, Uploaded, ErrorUpload, PauseUpload, Preparing, WaitingToUpload -> true
            PendingDownload, Downloading, Downloaded, ErrorDownload, PauseDownload, FilePathChanged, ThumbLoaded -> false
        }
    }

    private data class Entry(
        var transferData: TransferData? = null,
        val thumbs: MutableMap<ThumbKey, String?> = linkedMapOf()
    )

    private data class ThumbKey(val thumbFor: Int, val size: Size)

    private data class TransferKey(
        val messageTid: Long,
        val type: IdentityType,
        val value: String?,
    )

    private enum class IdentityType {
        Url,
        FilePath,
        Message
    }
}
