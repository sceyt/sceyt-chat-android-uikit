package com.sceyt.chatuikit.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.push.FirebaseMessagingDelegate
import kotlinx.coroutines.runBlocking


internal class SceytFirebaseMessageReceiver : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {
            runBlocking {
                FirebaseMessagingDelegate.handleRemoteMessage(remoteMessage)
            }
        } catch (exception: Exception) {
            SceytLog.e(TAG, "handleRemoteMessage error: " + exception.message.toString())
        }
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        FirebaseMessagingDelegate.registerFirebaseToken(s)
    }
}