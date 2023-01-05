package com.sceyt.sceytchatuikit.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.pushes.SceytFirebaseMessagingDelegate


internal class SceytFirebaseMessageReceiver : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {
            SceytFirebaseMessagingDelegate.handleRemoteMessage(remoteMessage)
        } catch (exception: Exception) {
            Log.e(TAG, exception.message.toString())
        }
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        SceytFirebaseMessagingDelegate.registerFirebaseToken(s)
    }
}