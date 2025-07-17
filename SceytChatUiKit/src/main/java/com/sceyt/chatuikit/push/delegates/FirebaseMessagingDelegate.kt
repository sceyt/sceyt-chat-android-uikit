package com.sceyt.chatuikit.push.delegates

import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDevice
import com.sceyt.chatuikit.push.PushServiceType
import com.sceyt.chatuikit.push.PushValidator

object FirebaseMessagingDelegate : MessagingDelegate() {

    @JvmStatic
    fun registerFirebaseToken(token: String) {
        pushService.registerPushDevice(PushDevice(token, PushServiceType.Fcm))
    }

    @JvmStatic
    fun unregisterFirebaseToken(unregisterPushCallback: ((Result<Boolean>) -> Unit)? = null) {
        pushService.unregisterPushDevice(unregisterPushCallback)
    }

    @JvmStatic
    fun handleRemoteMessage(remoteMessage: RemoteMessage): PushData? {
        if (!isChatPushNotification(remoteMessage)) {
            return null
        }
        val data = getDataFromRemoteMessage(remoteMessage) ?: return null
        pushService.handlePush(data)
        return data
    }

    @JvmStatic
    fun isChatPushNotification(remoteMessage: RemoteMessage): Boolean {
        return PushValidator.isChatPushNotification(remoteMessage.data)
    }

    @JvmStatic
    fun getDataFromRemoteMessage(remoteMessage: RemoteMessage): PushData? {
        return getDataFromPayload(remoteMessage.data)
    }
}