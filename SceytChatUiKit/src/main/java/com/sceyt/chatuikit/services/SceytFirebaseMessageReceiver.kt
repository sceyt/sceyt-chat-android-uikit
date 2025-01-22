package com.sceyt.chatuikit.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chatuikit.push.delegates.FirebaseMessagingDelegate


internal class SceytFirebaseMessageReceiver : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (FirebaseMessagingDelegate.isChatPushNotification(remoteMessage))
            FirebaseMessagingDelegate.handleRemoteMessage(remoteMessage)
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        FirebaseMessagingDelegate.registerFirebaseToken(s)
    }
}