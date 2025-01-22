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
    fun unregisterFirebaseToken(
            unregisterPushCallback: ((success: Boolean, error: String?) -> Unit)? = null
    ) {
        pushService.unregisterPushDevice(unregisterPushCallback)
    }

    @JvmStatic
    fun handleRemoteMessage(remoteMessage: RemoteMessage) {
        if (!isSceytPushNotification(remoteMessage)) {
            return
        }
        val data = getDataFromRemoteMessage(remoteMessage) ?: return
        pushService.handlePush(data)
    }

    @JvmStatic
    fun isSceytPushNotification(remoteMessage: RemoteMessage): Boolean {
        return PushValidator.isSceytPushNotification(remoteMessage.data)
    }

    @JvmStatic
    fun getDataFromRemoteMessage(remoteMessage: RemoteMessage): PushData? {
        return getDataFromPayload(remoteMessage.data)
    }
}