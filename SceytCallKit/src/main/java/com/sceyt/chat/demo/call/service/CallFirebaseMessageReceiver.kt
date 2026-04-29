package com.sceyt.chat.demo.call.service

import android.util.Log
import com.callclient.CallClient
import com.callclient.call.data.CallNotificationType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chatuikit.push.delegates.FirebaseMessagingDelegate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * FCM service that intercepts push notifications and routes them:
 *  - Call notifications  → [CallClient.handleNotification], then [CallManager.handleIncomingCall]
 *  - Chat notifications  → [FirebaseMessagingDelegate] (standard SDK path)
 */
class CallFirebaseMessageReceiver : FirebaseMessagingService(), KoinComponent {
    private val callManager by inject<CallManager>()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        when (val result = handleCallNotification(remoteMessage.data)) {
            is CallNotificationType.InviteToCall -> {
                Log.d(TAG, "Call invite push received — handling incoming call")
                callManager.handleIncomingCall(result.from, result.call)
            }

            is CallNotificationType.MissedCall,
            is CallNotificationType.JoinToCall -> {
                Log.d(TAG, "Call push received: $result")
            }

            is CallNotificationType.None -> {
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
