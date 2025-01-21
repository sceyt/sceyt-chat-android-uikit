package com.sceyt.chatuikit.persistence.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.persistence.logicimpl.usecases.ShouldShowNotificationUseCase
import com.sceyt.chatuikit.persistence.workers.HandlePushWorkManager.CHANNEL_ID
import com.sceyt.chatuikit.persistence.workers.HandlePushWorkManager.MESSAGE_ID
import com.sceyt.chatuikit.persistence.workers.HandlePushWorkManager.NOTIFICATION_TYPE
import com.sceyt.chatuikit.persistence.workers.HandlePushWorkManager.REACTION_ID
import com.sceyt.chatuikit.push.PushData
import org.koin.core.component.inject

internal object HandlePushWorkManager : SceytKoinComponent {

    internal const val CHANNEL_ID = "CHANNEL_ID"
    internal const val MESSAGE_ID = "MESSAGE_ID"
    internal const val USER_ID = "USER_ID"
    internal const val REACTION_ID = "REACTION_ID"
    internal const val NOTIFICATION_TYPE = "NOTIFICATION_TYPE"

    fun schedule(context: Context, data: Map<String, Any>): Operation {
        val inputData = Data.Builder()
            .putAll(data)
            .build()

        val myWorkRequest = OneTimeWorkRequest.Builder(HandlePushWorker::class.java)
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        return WorkManager.getInstance(context)
            .beginWith(myWorkRequest)
            .enqueue()
    }
}

internal class HandlePushWorker(
        context: Context,
        workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), SceytKoinComponent {
    private val shouldShowNotificationUseCase by inject<ShouldShowNotificationUseCase>()

    override suspend fun doWork(): Result {
        val data = inputData
        val notificationTypeValue = data.getInt(NOTIFICATION_TYPE, -1)
        val channelId = data.getLong(CHANNEL_ID, -1)
        val messageId = data.getLong(MESSAGE_ID, -1)
        val reactionId = data.getLong(REACTION_ID, -1)
        val type = NotificationType.entries.getOrNull(notificationTypeValue) ?: run {
            SceytLog.e(TAG, "HandlePushWorker worker was resumed with error: NotificationType is null with notificationTypeValue->${notificationTypeValue}")
            return Result.failure()
        }

        // If runAttemptCount is 0, it's indicating that the push notification is received for the first time
        if (runAttemptCount > 0)
            return Result.success()

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
                    SceytLog.e(TAG, "HandlePushWorker worker was resumed with error: Channel is null with channelId->${channelId}")
                    return Result.failure()
                }
        val message = SceytChatUIKit.chatUIFacade.messageInteractor.getMessageDbById(messageId)
                ?: run {
                    SceytLog.e(TAG, "HandlePushWorker worker was resumed with error: Message is null with message->${messageId}")
                    return Result.failure()
                }

        var reaction: SceytReaction? = null
        if (type == NotificationType.MessageReaction) {
            reactionId?.let {
                reaction = SceytChatUIKit.chatUIFacade.messageReactionInteractor.getLocalMessageReactionsById(
                    reactionId = reactionId
                )
            } ?: run {
                SceytLog.e(TAG, "HandlePushWorker worker was resumed with error: ReactionId is null")
                Result.failure()
            }
        }

        if (shouldShowNotificationUseCase(type, channel, message, reaction)) {
            SceytChatUIKit.notifications.pushNotification.pushNotificationHandler.showNotification(
                context = applicationContext,
                data = PushData(
                    type = type,
                    channel = channel,
                    message = message,
                    user = message.user ?: return Result.failure(),
                    reaction = reaction
                ))
        }

        //   connectionProvider.connectChatClient()

        /*  if (ConnectionEventManager.awaitToConnectSceytWithTimeout(1.minutes.inWholeMilliseconds)) {
              val result = SceytChatUIKit.chatUIFacade.messageInteractor.markMessagesAs(
                  channelId,
                  MarkerType.Received,
                  messageId
              ).getOrNull(0)
              if (result is SceytResponse.Success) {
                  SceytLog.i(TAG, "Sent ack receive for Id: ${message.id} body: ${message.body} succeeded")
              } else SceytLog.e(TAG, "Failed to send ack received for msgId: ${message.id} body: ${message.body}  error: ${result?.message}")
          }*/

        return Result.success()
    }
}
