package com.sceyt.chatuikit.persistence.file_transfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

/** Coordinates attachment transfers and owns their registered [TransferTask] instances. */
interface FileTransferService {
    /**
     * Starts or rejoins an upload and returns its registered task.
     *
     * @param shared allows uploads of the same source file to share a transfer.
     * @param configureTask configures callbacks and listeners before the upload starts or rejoins.
     */
    fun upload(
        attachment: SceytAttachment,
        shared: Boolean = false,
        configureTask: TransferTask.() -> Unit = {},
    ): TransferTask

    /**
     * Starts or rejoins a download and returns its registered task.
     *
     * @param configureTask configures callbacks and listeners before the download starts or rejoins.
     */
    fun download(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit = {},
    ): TransferTask

    /** Pauses the upload or download selected by [state], retaining it for [resume]. */
    fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState)

    /**
     * Resumes or retries the upload or download selected by [state].
     * Supply [channelId] for uploads so channel cleanup also cancels the resumed worker.
     */
    fun resume(
        messageTid: Long,
        attachment: SceytAttachment,
        state: TransferState,
        channelId: Long? = null,
    )

    /**
     * Cancels this attachment's upload and download and requests cleanup of retained transfer data.
     * Other messages sharing the upload are preserved.
     */
    fun cancelUpload(attachment: SceytAttachment)

    /** Loads or generates a thumbnail and delivers it through [TransferTask.thumbCallback]. */
    fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData)

    /** Returns the registered task, creating and registering it when absent. */
    fun findOrCreateTransferTask(attachment: SceytAttachment): TransferTask

    /** Returns the registered task, or null when absent. */
    fun findTransferTask(attachment: SceytAttachment): TransferTask?

    /** Removes only this task, leaving other attachments and newer attempts registered. */
    fun removeTransferTask(task: TransferTask)

    /** Returns a live view of registered tasks, keyed by message TID. */
    fun getTasks(): Map<String, TransferTask>

    /** Clears thumbnail preparation tracking so thumbnails can be requested again. */
    fun clearPreparingThumbPaths()

    /**
     * Cancels registered uploads and downloads, clears transfer and thumbnail tracking,
     * and fails waiting completion listeners.
     */
    fun cancelAllTransfers()

    /**
     * Resets a finished attachment with a missing local file and starts a new download.
     * Existing pending, running, or paused downloads are preserved.
     */
    fun redownload(attachment: SceytAttachment)
}
