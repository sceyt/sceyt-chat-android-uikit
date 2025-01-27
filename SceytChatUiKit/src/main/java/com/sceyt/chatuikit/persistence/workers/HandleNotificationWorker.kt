package com.sceyt.chatuikit.persistence.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sceyt.chat.ChatClient
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.persistence.logicimpl.usecases.ShouldShowNotificationUseCase
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorkManager.CHANNEL_ID
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorkManager.MESSAGE_ID
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorkManager.NOTIFICATION_TYPE
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorkManager.REACTION_ID
import com.sceyt.chatuikit.push.PushData
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes

internal object HandleNotificationWorkManager {

    internal const val CHANNEL_ID = "CHANNEL_ID"
    internal const val MESSAGE_ID = "MESSAGE_ID"
    internal const val USER_ID = "USER_ID"
    internal const val REACTION_ID = "REACTION_ID"
    internal const val NOTIFICATION_TYPE = "NOTIFICATION_TYPE"

    fun schedule(context: Context, data: Map<String, Any>): Operation {
        val inputData = Data.Builder()
            .putAll(data)
            .build()

        val myWorkRequest = OneTimeWorkRequest.Builder(HandleNotificationWorker::class.java)
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        return WorkManager.getInstance(context)
            .beginWith(myWorkRequest)
            .enqueue()
    }
}

internal class HandleNotificationWorker(
        context: Context,
        workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), SceytKoinComponent {
    private val shouldShowNotificationUseCase by inject<ShouldShowNotificationUseCase>()
    private val pushNotificationHandler by lazy {
        SceytChatUIKit.notifications.pushNotification.notificationHandler
    }

    override suspend fun doWork(): Result {
        val data = inputData
        val notificationTypeValue = data.getInt(NOTIFICATION_TYPE, -1)
        val channelId = data.getLong(CHANNEL_ID, -1)
        val messageId = data.getLong(MESSAGE_ID, -1)
        val reactionId = data.getLong(REACTION_ID, -1)
        val type = NotificationType.entries.getOrNull(notificationTypeValue) ?: run {
            return finishWorkWithFailure("NotificationType is null with notificationTypeValue->${notificationTypeValue}")
        }

        // If runAttemptCount is 0, it's indicating that the push notification is received for the first time
        if (runAttemptCount > 0)
            return finishWorkWithSuccess()

        return getDataAndShowNotificationIfNeeded(
            type = type,
            channelId = channelId,
            messageId = messageId,
            reactionId = reactionId
        )
    }

    private suspend fun getDataAndShowNotificationIfNeeded(
            type: NotificationType,
            channelId: Long,
            messageId: Long,
            reactionId: Long? = null
    ): Result {
        SceytLog.i(TAG, "Got a notification with chatId: $channelId, messageId: $messageId ")

        val channel = SceytChatUIKit.chatUIFacade.channelInteractor.getChannelFromDb(channelId)
                ?: run {
                    return finishWorkWithFailure("Channel not found: $channelId")
                }
        val message = SceytChatUIKit.chatUIFacade.messageInteractor.getMessageFromDb(messageId)
                ?: run {
                    return finishWorkWithFailure("Message not found: $messageId")
                }

        var reaction: SceytReaction? = null
        if (type == NotificationType.MessageReaction) {
            reactionId?.let {
                reaction = SceytChatUIKit.chatUIFacade.messageReactionInteractor.getLocalMessageReactionsById(
                    reactionId = reactionId
                )
            } ?: run {
                return finishWorkWithFailure("Reaction not found, but type is MessageReaction")
            }
        }

        if (shouldShowNotificationUseCase(type, channel, message, reaction)) {
            pushNotificationHandler.showNotification(
                context = applicationContext,
                data = PushData(
                    type = type,
                    channel = channel,
                    message = message,
                    user = message.user
                            ?: return finishWorkWithFailure("Message sender not found: $messageId"),
                    reaction = reaction
                ))
        }

        if (ConnectionEventManager.isConnected) {
            SceytLog.i(TAG, "SceytChat is connected. Marking message as received: $messageId")
            markMessageAsReceived(channelId, message)
        } else {
            SceytLog.i(TAG, "SceytChat is not connected. Connecting to mark message as received: $messageId")
            val token = SceytChatUIKit.chatTokenProvider?.provideToken().takeIf { !it.isNullOrBlank() }
                    ?: run {
                        return finishWorkWithFailure("Couldn't get token to connect to mark message as received: $messageId")
                    }

            ChatClient.getClient().connect(token)

            if (ConnectionEventManager.awaitToConnectSceytWithTimeout(1.minutes.inWholeMilliseconds)) {
                markMessageAsReceived(channelId, message)
            }
        }
        return finishWorkWithSuccess()
    }

    private suspend fun markMessageAsReceived(channelId: Long, message: SceytMessage) {
        val result = SceytChatUIKit.chatUIFacade.messageInteractor.markMessagesAs(
            channelId,
            MarkerType.Received,
            message.id
        ).firstOrNull()

        if (result is SceytResponse.Success) {
            SceytLog.i(TAG, "Sent ack receive for Id: ${message.id} succeeded")
        } else SceytLog.e(TAG, "Failed to send ack received for msgId: ${message.id} error: ${result?.message}")
    }

    private fun finishWorkWithFailure(error: String): Result {
        val result = Result.success()
        SceytLog.e(TAG, "HandlePushWorker worker finished with error: $error")
        pushNotificationHandler.notificationWorkerFinished(result = result)
        return result
    }

    private fun finishWorkWithSuccess(): Result {
        val result = Result.success()
        pushNotificationHandler.notificationWorkerFinished(result = result)
        return Result.success()
    }

    companion object {
        private const val TAG = "HandlePushWorker"
    }
}
