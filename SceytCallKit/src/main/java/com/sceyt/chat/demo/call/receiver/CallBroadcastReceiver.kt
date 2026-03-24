package com.sceyt.chat.demo.call.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sceyt.chat.demo.call.manager.CallManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Handles broadcast intents from call notifications.
 */
class CallBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        private const val TAG = "CallBroadcastReceiver"

        // Note: ACTION_ANSWER is intentionally absent — the notification "Answer" action uses
        // PendingIntent.getActivity() → CallActivity instead, because Android 10+ blocks
        // background activity starts from BroadcastReceiver.
        const val ACTION_DECLINE = "com.sceyt.call.ACTION_DECLINE"
        const val ACTION_END_CALL = "com.sceyt.call.ACTION_END_CALL"
        const val ACTION_TOGGLE_MUTE = "com.sceyt.call.ACTION_TOGGLE_MUTE"
    }

    private val callManager: CallManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received action: ${intent.action}")

        when (intent.action) {
            ACTION_DECLINE -> handleDecline()
            ACTION_END_CALL -> handleEndCall()
            ACTION_TOGGLE_MUTE -> handleToggleMute()
        }
    }

    private fun handleDecline() {
        callManager.declineIncomingCall()
    }

    private fun handleEndCall() {
        callManager.endCall()
    }

    private fun handleToggleMute() {
        callManager.toggleMute()
    }
}
