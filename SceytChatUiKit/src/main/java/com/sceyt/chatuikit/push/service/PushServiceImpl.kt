package com.sceyt.chatuikit.push.service

import android.content.Context
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorkManager
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class PushServiceImpl(
        private val context: Context,
        private val scope: CoroutineScope,
        private val messagesLogic: PersistenceMessagesLogic,
) : PushService {

    override fun handlePush(data: PushData) {
        SceytLog.d(TAG, "Handling push for messageId: ${data.message.id}, channelId: ${data.message.channelId}")
        scope.launch {
            // At first, we call the handlePush method, which will save the message to the database
            if (!messagesLogic.handlePush(data)) return@launch

            val config = SceytChatUIKit.config.notificationConfig
            if (config.isPushEnabled && config.shouldDisplayNotification(data)) {
                val workerData = mutableMapOf<String, Any>(
                    HandleNotificationWorkManager.NOTIFICATION_TYPE to data.type.ordinal,
                    HandleNotificationWorkManager.CHANNEL_ID to data.channel.id,
                    HandleNotificationWorkManager.MESSAGE_ID to data.message.id,
                    HandleNotificationWorkManager.USER_ID to data.user.id,
                )
                data.reaction?.let {
                    workerData[HandleNotificationWorkManager.REACTION_ID] = it.id
                }
                HandleNotificationWorkManager.schedule(context, workerData)
            }
        }
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
        ChatClient.getClient().registerPushToken(device.token, device.service.stingValue(), object : ActionCallback {
            override fun onSuccess() {
                SceytLog.i(TAG, "Push token successfully registered, service: ${device.service}")
            }

            override fun onError(e: SceytException) {
                SceytLog.e(TAG, "Couldn't register push token: service: ${device.service}, error: $e")
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
                unregisterPushCallback?.invoke(Result.failure(exception
                        ?: Exception("Unknown error")))
            }
        })
    }

    companion object {
        private const val TAG = "PushService"
    }
}