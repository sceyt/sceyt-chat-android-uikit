package com.sceyt.sceytchatuikit.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.pushes.SceytFirebaseMessagingDelegate
import kotlinx.coroutines.runBlocking


internal class SceytFirebaseMessageReceiver : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {
            runBlocking {
                SceytFirebaseMessagingDelegate.handleRemoteMessage(remoteMessage)
            }
        } catch (exception: Exception) {
            SceytLog.e(TAG, "handleRemoteMessage error: " + exception.message.toString())
        }
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        SceytFirebaseMessagingDelegate.registerFirebaseToken(s)
    }
}