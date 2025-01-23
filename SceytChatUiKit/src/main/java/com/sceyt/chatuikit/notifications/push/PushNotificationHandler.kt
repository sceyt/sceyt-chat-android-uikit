package com.sceyt.chatuikit.notifications.push

import android.content.Context
import androidx.work.ListenableWorker
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.notifications.NotificationHandler
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorker
import com.sceyt.chatuikit.push.PushData

interface PushNotificationHandler : NotificationHandler<PushData> {
    /**
     * Handles the completion of the notification worker.
     *
     * This method is called when the [HandleNotificationWorker]` has finished its task.
     *
     * @param result The result of the worker's operation, indicating success or failure.
     */
    fun notificationWorkerFinished(result: ListenableWorker.Result) {}

    /**
     * Handles the "Mark as Read" notification action.
     *
     * This method is triggered when the user interacts with the "Mark as Read" action
     * in a notification.
     */
    fun onMarkAsReadAction() {}

    /**
     * Handles the "Reply" notification action.
     *
     * This method is triggered when the user interacts with the "Reply" action
     * in a notification.
     *
     * @param message The message to send as a reply.
     */
    fun onReplyAction(message: CharSequence) {}

    /**
     * Handles the completion of the "Mark as Read" notification action.
     *
     * This method is called after the "Mark as Read" action for a notification is completed.
     *
     * @param result A [Result] containing a [Boolean] value indicating whether
     *               the operation was successful (`true`) or not (`false`).
     */
    fun markUsReadActionFinished(result: Result<Boolean>) {}

    /**
     * Handles the completion of the "Reply" notification action.
     *
     * This method is called after the "Reply" action for a notification is completed.
     *
     * @param result A [Result] containing a [Boolean] value indicating whether
     *               the operation was successful (`true`) or not (`false`).
     */
    fun replyActionFinished(result: Result<Boolean>) {}

    /**
     * Called when the state of a message changes.
     *
     * This method is triggered to handle updates related to message state changes,
     * such as edited or deleted.
     *
     * @param context The context used to access resources or services.
     * @param message The [SceytMessage] whose state has changed.
     */
    suspend fun onMessageStateChanged(context: Context, message: SceytMessage)

    /**
     * Called when a reaction is deleted from a message.
     *
     * This method is triggered to handle updates when a reaction is removed from a message.
     *
     * @param context The context used to access resources or services.
     * @param message The [SceytMessage] associated with the deleted reaction.
     * @param reaction The [SceytReaction] that was deleted.
     */
    suspend fun onReactionDeleted(context: Context, message: SceytMessage, reaction: SceytReaction)
}
