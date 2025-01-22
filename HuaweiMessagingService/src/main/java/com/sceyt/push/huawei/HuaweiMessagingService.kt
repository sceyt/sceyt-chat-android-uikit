package com.sceyt.push.huawei

import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

class HuaweiMessagingService : HmsMessageService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (HuaweiMessagingDelegate.isSceytPushNotification(remoteMessage))
            HuaweiMessagingDelegate.handleRemoteMessage(remoteMessage)
    }

    override fun onNewToken(token: String) {
        HuaweiMessagingDelegate.registerFirebaseToken(token)
    }
}
