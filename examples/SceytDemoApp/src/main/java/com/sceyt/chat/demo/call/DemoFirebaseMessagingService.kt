package com.sceyt.chat.demo.call

import com.callclient.logger.CallLog
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.calluikit.push.CallPushHandler
import com.sceyt.chatuikit.push.delegates.FirebaseMessagingDelegate

/** Routes both call and chat pushes through their respective UI Kits. */
class DemoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (CallPushHandler.isCallPush(remoteMessage.data)) {
            runCatching { CallPushHandler.handle(remoteMessage.data) }
                .onFailure { CallLog.e(TAG, "Couldn't handle call push notification", it) }
        } else {
            FirebaseMessagingDelegate.handleRemoteMessage(remoteMessage)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseMessagingDelegate.registerFirebaseToken(token)
    }

    private companion object {
        const val TAG = "DemoFirebaseMessaging"
    }
}
