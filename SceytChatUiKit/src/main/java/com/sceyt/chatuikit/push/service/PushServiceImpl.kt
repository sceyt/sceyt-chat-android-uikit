package com.sceyt.chatuikit.push.service

import android.content.Context
import androidx.work.await
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorkManager
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDevice
import com.sceyt.chatuikit.push.PushHandleResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class PushServiceImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    private val messagesLogic: PersistenceMessagesLogic,
) : PushService {

    override fun handlePush(data: PushData, callback: ((PushHandleResult) -> Unit)?) {
        scope.launch {
            val result = handlePushSuspended(data)
            callback?.invoke(result)
        }
    }

    override suspend fun handlePushSuspended(data: PushData): PushHandleResult {
        SceytLog.d(
            TAG,
            "Handling push for messageId: ${data.message.id}, channelId: ${data.message.channelId}"
        )
        return try {
            // At first, we call the handlePush method, which will save the message to the database
            if (!messagesLogic.handlePush(data)) {
                SceytLog.d(TAG, "Push skipped by persistence, messageId: ${data.message.id}")
                return PushHandleResult.Skipped(data)
            }

            val config = SceytChatUIKit.config.notificationConfig
            val shouldDisplay = config.isPushEnabled && config.shouldDisplayNotification(data)
            if (shouldDisplay) {
                scheduleNotificationWork(data)
            }
            PushHandleResult.Handled(data = data, notificationScheduled = shouldDisplay)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            SceytLog.e(TAG, "Couldn't handle push for messageId: ${data.message.id}, error: $e")
            PushHandleResult.Failed(data = data, throwable = e)
        }
    }

    /** Enqueues the notification work and suspends until the enqueue itself completes. */
    private suspend fun scheduleNotificationWork(data: PushData) {
        val workerData = mutableMapOf<String, Any>(
            HandleNotificationWorkManager.NOTIFICATION_TYPE to data.type.ordinal,
            HandleNotificationWorkManager.CHANNEL_ID to data.channel.id,
            HandleNotificationWorkManager.MESSAGE_ID to data.message.id,
            HandleNotificationWorkManager.USER_ID to data.user.id,
        )
        data.reaction?.let {
            workerData[HandleNotificationWorkManager.REACTION_ID] = it.id
        }
        HandleNotificationWorkManager.schedule(context, workerData).await()
    }

    override fun registerPushDevice(device: PushDevice) {
        registerClientPushTokenImpl(device)
    }

    override fun unregisterPushDevice(unregisterPushCallback: ((Result<Boolean>) -> Unit)?) {
        unregisterClientPushTokenImpl(unregisterPushCallback)
    }

    override fun ensurePushTokenRegistered() {
        asyncGetDeviceToken(::registerClientPushTokenImpl)
    }

    private fun asyncGetDeviceToken(onToken: (device: PushDevice) -> Unit) {
        SceytChatUIKit.config.notificationConfig.pushProviders.firstOrNull {
            it.isSupported(context)
        }?.generatePushDeviceAsync(onToken)
    }

    private fun registerClientPushTokenImpl(device: PushDevice) {
        val pushSubscriptions = ChatClient.getClient().pushSubscriptions
        val registered = pushSubscriptions.any {
            it.dataToken == device.token && it.service == device.service.stingValue()
        }
        if (registered) return
        ChatClient.getClient().registerPushToken(
            /* token = */ device.token,
            /* service = */ device.service.stingValue(),
            /* actionCallback = */ object : ActionCallback {
                override fun onSuccess() {
                    SceytLog.i(
                        TAG, "Push token successfully registered, service: ${device.service}"
                    )
                }

                override fun onError(e: SceytException) {
                    SceytLog.e(
                        TAG, "Couldn't register push token: service: ${device.service}, error: $e"
                    )
                }
            })
    }

    private fun unregisterClientPushTokenImpl(
        unregisterPushCallback: ((Result<Boolean>) -> Unit)?,
    ) {
        ChatClient.getClient().unregisterPushToken(object : ActionCallback {
            override fun onSuccess() {
                unregisterPushCallback?.invoke(Result.success(true))
            }

            override fun onError(exception: SceytException?) {
                unregisterPushCallback?.invoke(
                    Result.failure(exception ?: Exception("Unknown error"))
                )
            }
        })
    }

    companion object {
        private const val TAG = "PushService"
    }
}