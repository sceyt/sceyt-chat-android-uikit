package com.sceyt.chat.demo.call.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.callclient.CallClient
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.notification.CallNotificationManager
import com.sceyt.chat.demo.call.ui.CallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Handles broadcast intents from call notifications.
 */
class CallBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        private const val TAG = "CallBroadcastReceiver"

        const val ACTION_ANSWER = "com.sceyt.call.ACTION_ANSWER"
        const val ACTION_DECLINE = "com.sceyt.call.ACTION_DECLINE"
        const val ACTION_END_CALL = "com.sceyt.call.ACTION_END_CALL"
        const val ACTION_TOGGLE_MUTE = "com.sceyt.call.ACTION_TOGGLE_MUTE"
    }

    private val callManager: CallManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received action: ${intent.action}")
        val callId = intent.getStringExtra(CallNotificationManager.CALL_ID) ?: return

        when (intent.action) {
            ACTION_ANSWER -> handleAnswer(context,callId)
            ACTION_DECLINE -> handleDecline()
            ACTION_END_CALL -> handleEndCall()
            ACTION_TOGGLE_MUTE -> handleToggleMute()
        }
    }

    private fun handleAnswer(context: Context, callId: String) {
        scope.launch {
            val result = callManager.answerIncomingCall(
                CallClient.instance?.getOngoingCall(callId) ?: return@launch
            )
            if (result.isSuccess) {
                // Launch call activity
                context.startActivity(CallActivity.createOngoingIntent(context).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
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
