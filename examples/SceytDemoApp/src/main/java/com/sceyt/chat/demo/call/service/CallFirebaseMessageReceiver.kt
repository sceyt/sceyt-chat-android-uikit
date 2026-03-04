package com.sceyt.chat.demo.call.service

import android.util.Log
import com.callclient.CallClient
import com.callclient.call.data.CallNotificationType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chat.demo.call.worker.CallWorker
import com.sceyt.chatuikit.push.delegates.FirebaseMessagingDelegate

/**
 * FCM service that intercepts push notifications and routes them:
 *  - Call notifications  → [CallClient.handleNotification], then start [CallWorker] on invite
 *  - Chat notifications  → [FirebaseMessagingDelegate] (standard SDK path)
 */
class CallFirebaseMessageReceiver : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        when (val notificationType = handleCallNotification(remoteMessage.data)) {
            CallNotificationType.InviteToCall -> {
                // SDK has processed the invite internally and will fire
                // CallClient.ClientListener.onInvitedToCall(), which sets CallManager state.
                Log.d(TAG, "Call invite push received — starting CallWorker")
            }

            CallNotificationType.MissedCall,
            CallNotificationType.JoinToCall -> {
                Log.d(TAG, "Call push received: $notificationType")
            }

            CallNotificationType.None -> {
                // Not a call notification — delegate to the chat SDK
                if (FirebaseMessagingDelegate.isChatPushNotification(remoteMessage)) {
                    FirebaseMessagingDelegate.handleRemoteMessage(remoteMessage)
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        FirebaseMessagingDelegate.registerFirebaseToken(token)
    }

    private fun handleCallNotification(data: Map<String, String>): CallNotificationType {
        return try {
            CallClient.requireInstance().handleNotification(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling call notification", e)
            CallNotificationType.None
        }
    }

    companion object {
        private const val TAG = "CallFirebaseMessageReceiver"
    }
}
