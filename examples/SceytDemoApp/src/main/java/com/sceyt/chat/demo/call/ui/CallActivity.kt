package com.sceyt.chat.demo.call.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.ui.screens.CallScreen
import com.sceyt.chat.demo.call.ui.theme.CallTheme
import com.sceyt.chat.demo.call.worker.CallWorker
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Activity that hosts call UI screens using Jetpack Compose.
 * Supports full-screen incoming call display and ongoing call management.
 */
class CallActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_CALL_TYPE = "extra_call_type"
        private const val EXTRA_CALLER_ID = "extra_caller_id"
        private const val EXTRA_IS_VIDEO = "extra_is_video"

        private const val CALL_TYPE_INCOMING = "incoming"
        private const val CALL_TYPE_OUTGOING = "outgoing"
        private const val CALL_TYPE_ONGOING = "ongoing"

        /**
         * Creates intent to launch for incoming call (full-screen).
         */
        fun createIncomingIntent(context: Context): Intent {
            return Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_CALL_TYPE, CALL_TYPE_INCOMING)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        /**
         * Creates intent to launch for outgoing call.
         */
        fun createOutgoingIntent(
            context: Context,
            userId: String,
            isVideo: Boolean
        ): Intent {
            return Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_CALL_TYPE, CALL_TYPE_OUTGOING)
                putExtra(EXTRA_CALLER_ID, userId)
                putExtra(EXTRA_IS_VIDEO, isVideo)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        /**
         * Creates intent to return to ongoing call.
         */
        fun createOngoingIntent(context: Context): Intent {
            return Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_CALL_TYPE, CALL_TYPE_ONGOING)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        /**
         * Launches incoming call UI.
         */
        fun launchIncoming(context: Context, callerId: String, isVideo: Boolean) {
            val intent = createIncomingIntent(context).apply {
                putExtra(EXTRA_CALLER_ID, callerId)
                putExtra(EXTRA_IS_VIDEO, isVideo)
            }
            context.startActivity(intent)
        }

        /**
         * Launches outgoing call UI.
         */
        fun launchOutgoing(context: Context, userId: String, isVideo: Boolean) {
            context.startActivity(createOutgoingIntent(context, userId, isVideo))
        }
    }

    private val viewModel: CallViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup window for full-screen calls
        setupWindowFlags()

        enableEdgeToEdge()

        // Start foreground worker for call persistence
        CallWorker.start(this)

        // Send ringing signal if this is an incoming call
        if (intent.getStringExtra(EXTRA_CALL_TYPE) == CALL_TYPE_INCOMING) {
            viewModel.sendRinging()
        }

        setContent {
            CallTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val callState by viewModel.callUiState.collectAsState()

                    CallScreen(
                        viewModel = viewModel,
                        onDismiss = { finishAndRemoveTask() }
                    )

                    // Auto-close on Idle:
                    // - LocalHangup: immediate dismiss via CallScreen LaunchedEffect
                    // - RemoteHangup: brief "Call Ended" screen (2s), then transitions to Idle here
                    // - Failed/Declined/NoAnswer: EndedCallScreen shown, dismissed via endedDismissJob
                    if (callState.phase == CallUiState.CallPhase.Idle) {
                        finishAndRemoveTask()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Activity is being brought back to foreground
    }

    private fun setupWindowFlags() {
        val callType = intent.getStringExtra(EXTRA_CALL_TYPE)

        if (callType == CALL_TYPE_INCOMING) {
            // Show over lock screen for incoming calls
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }

        // Keep screen on during call
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Worker will stop itself when call ends
    }
}
