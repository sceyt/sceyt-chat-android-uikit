package com.sceyt.chatuikit.push.service

import android.content.Context
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.repositories.Keys.KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.persistence.workers.HandleNotificationWorkManager
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class PushServiceImpl(
        private val context: Context,
        private val scope: CoroutineScope,
        private val messagesLogic: PersistenceMessagesLogic,
        private val preferences: SceytSharedPreference,
) : PushService {

    override fun handlePush(data: PushData) {
        SceytLog.d(TAG, "Handling push: ${data.message.id} ${data.message.body}")
        scope.launch {
            // At first, we call the handlePush, which will save the message to the database
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

    override fun unregisterPushDevice(
            unregisterPushCallback: ((success: Boolean, error: String?) -> Unit)?
    ) {
        ChatClient.getClient().unregisterPushToken(object : ActionCallback {
            override fun onSuccess() {
                preferences.setBoolean(KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION, false)
                unregisterPushCallback?.invoke(true, null)
            }

            override fun onError(exception: SceytException?) {
                unregisterPushCallback?.invoke(false, exception?.message)
            }
        })
    }

    override fun ensurePushTokenRegistered() {
        if (!preferences.getBoolean(KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION)) {
            asyncGetDeviceToken(::registerClientPushTokenImpl)
        }
    }

    private fun asyncGetDeviceToken(onToken: (device: PushDevice) -> Unit) {
        SceytChatUIKit.config.notificationConfig.pushProviders.firstOrNull { it.isSupported(context) }?.let {
            it.generatePushDeviceAsync { pushDevice ->
                onToken(pushDevice)
            }
        }
    }

    private fun registerClientPushTokenImpl(device: PushDevice) {
        ChatClient.getClient().registerPushToken(device.token, device.service.stingValue(), object : ActionCallback {
            override fun onSuccess() {
                preferences.setBoolean(KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION, true)
                SceytLog.i(this@PushServiceImpl.TAG, "Push token successfully registered, service: ${device.service}")
            }

            override fun onError(e: SceytException) {
                SceytLog.e(this@PushServiceImpl.TAG, "Couldn't register push token: service: ${device.service}, error: $e")
            }
        })
    }
}