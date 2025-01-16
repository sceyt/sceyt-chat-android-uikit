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
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.isAppOnForeground
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorkManager.CHANNEL_ID
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorkManager.MESSAGE_ID
import com.sceyt.chatuikit.push.PushData

internal object HandleNotificationWorkManager : SceytKoinComponent {

    internal const val CHANNEL_ID = "CHANNEL_ID"
    internal const val MESSAGE_ID = "MESSAGE_ID"
    internal const val USER_ID = "USER_ID"

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

    override suspend fun doWork(): Result {
        val data = inputData
        val channelId = data.getLong(CHANNEL_ID, -1)
        val messageId = data.getLong(MESSAGE_ID, -1)
        return processChatMessages(messageId = messageId, channelId = channelId)
    }

    private suspend fun processChatMessages(
            messageId: Long,
            channelId: Long
    ): Result {
        SceytLog.i(TAG, "Got a notification with chatId: $channelId, messageId: $messageId ")

        // When app is in foreground and chat client is connected, app will handle notification
        if (ConnectionEventManager.isConnected || ChannelsCache.currentChannelId == channelId) {
            SceytLog.i(TAG, "App is on foreground and chat client is connected, ignore worker chatId: $channelId, messageId: $messageId ")
            return Result.success()
        }

        val shouldShowNotification = runAttemptCount == 0
        val channel = SceytChatUIKit.chatUIFacade.channelInteractor.getChannelFromDb(channelId)
                ?: run {
                    SceytLog.e(TAG, "HandleNotificationWorkManager worker was resumed with error: Channel is null with channelId->${channelId}")
                    return Result.failure()
                }
        val message = SceytChatUIKit.chatUIFacade.messageInteractor.getMessageDbById(messageId)
                ?: run {
                    SceytLog.e(TAG, "HandleNotificationWorkManager worker was resumed with error: Message is null with message->${messageId}")
                    return Result.failure()
                }

        if (message.createdAt <= channel.messagesClearedAt || message.id <= channel.messagesClearedAt) {
            SceytLog.e(TAG, "HandleNotificationWorkManager worker was resumed with error: Message is cleared with message->${messageId}")
            return Result.failure()
        }

        if (shouldShowNotification
                && !channel.muted
                && (ChannelsCache.currentChannelId != channelId || !applicationContext.isAppOnForeground())
        ) {
            SceytChatUIKit.notifications.notificationHandler.showNotification(
                context = applicationContext,
                data = PushData(
                    channel = channel,
                    message = message,
                    user = message.user ?: return Result.failure(),
                    reaction = null
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
