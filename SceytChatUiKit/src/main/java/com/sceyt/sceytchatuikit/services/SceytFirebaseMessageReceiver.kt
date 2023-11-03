package com.sceyt.sceytchatuikit.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.pushes.SceytFirebaseMessagingDelegate
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


internal class SceytFirebaseMessageReceiver : FirebaseMessagingService() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {
            GlobalScope.launch {
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