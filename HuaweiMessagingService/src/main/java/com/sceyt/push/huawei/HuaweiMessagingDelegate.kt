package com.sceyt.push.huawei

import com.huawei.hms.push.RemoteMessage
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDevice
import com.sceyt.chatuikit.push.PushServiceType
import com.sceyt.chatuikit.push.PushValidator
import com.sceyt.chatuikit.push.delegates.MessagingDelegate

object HuaweiMessagingDelegate : MessagingDelegate() {

    @JvmStatic
    fun registerFirebaseToken(token: String) {
        pushService.registerPushDevice(PushDevice(token, PushServiceType.Hms))
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
        return PushValidator.isSceytPushNotification(remoteMessage.dataOfMap)
    }

    @JvmStatic
    fun getDataFromRemoteMessage(remoteMessage: RemoteMessage): PushData? {
        return getDataFromPayload(remoteMessage.dataOfMap)
    }
}