package com.sceyt.chatuikit.notifications.service

import androidx.work.ListenableWorker
import com.sceyt.chatuikit.notifications.NotificationHandler
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorker

interface FileTransferNotificationHandler : NotificationHandler<FileTransferNotificationData> {
    /**
     * Handles the completion of the notification worker.
     *
     * This method is called when the [UploadAndSendAttachmentWorker]` has finished its task.
     *
     * @param result The result of the worker's operation, indicating success or failure.
     */
    fun serviceWorkerFinished(result: ListenableWorker.Result) {}
}