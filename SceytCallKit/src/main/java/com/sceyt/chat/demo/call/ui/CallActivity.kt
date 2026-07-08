package com.sceyt.chat.demo.call.ui

import android.Manifest
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.isGroupCall
import com.sceyt.chat.demo.call.manager.isVideoCall
import com.sceyt.chat.demo.call.ui.screens.CallScreen
import com.sceyt.chat.demo.call.ui.theme.CallTheme
import com.sceyt.chatuikit.extensions.applySystemBarsStyle
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Activity that hosts call UI screens using Jetpack Compose.
 * Supports full-screen incoming call display, ongoing call management, and PiP mode.
 */
class CallActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModel()

    private val isPipMode = MutableStateFlow(false)

    // Used for the notification "Answer" action — requests mic (required) and camera (video only).
    // Answer proceeds only if microphone is granted.
    private val autoAnswerPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.onAnswerClick()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup window for full-screen calls
        setupWindowFlags()

        applySystemBarsStyle(true, android.R.color.transparent, android.R.color.transparent)

        // Enter PiP instead of finishing when Back is pressed during an active call
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!goToPipModeOrFinish()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Send ringing signal if this is an incoming call
        if (intent.getStringExtra(EXTRA_CALL_TYPE) == CALL_TYPE_INCOMING) {
            viewModel.sendRinging()
        }

        // Answer immediately if launched via notification "Answer" action
        if (intent.getBooleanExtra(EXTRA_AUTO_ANSWER, false)) {
            answerWithPermissionsIfNeeded()
        }

        setContent {
            CallTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val callState by viewModel.callUiState.collectAsState()
                    val isInPip by isPipMode.collectAsState()

                    CallScreen(
                        viewModel = viewModel,
                        isInPipMode = isInPip,
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

    override fun onResume() {
        super.onResume()
        // Keep PiP params up-to-date so Android S+ auto-enter stays configured
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updatePipParams()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Answer immediately if activity was already running when user tapped "Answer"
        if (intent.getBooleanExtra(EXTRA_AUTO_ANSWER, false)) {
            answerWithPermissionsIfNeeded()
        }
    }

    /**
     * Re-applies status bar appearance after PIP transition. The system resets window flags
     * during the PIP configuration change, after Compose's SideEffect has already run.
     * onWindowFocusChanged fires last — after all transitions have settled.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
                false
        }
    }

    /** Called when the user presses Home on pre-S devices (S+ uses setAutoEnterEnabled). */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            goToPipModeOrFinish()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipMode.value = isInPictureInPictureMode

        if (!isInPictureInPictureMode) {
            // User dismissed the PiP window via the close (X) button —
            // lifecycle is CREATED (not STARTED/RESUMED) in this case
            if (lifecycle.currentState == Lifecycle.State.CREATED) {
                viewModel.onAppBackground() // turn off local video before closing
                finishAndRemoveTask()
                return
            }
            // User expanded PiP back to full screen — restore camera
            viewModel.onAppForeground()
        }
    }

    /**
     * Enters PiP if the system allows it and the call is in an active phase.
     * Returns true if PiP was entered, false if the caller should handle the event normally.
     */
    private fun goToPipModeOrFinish(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (!isPipAllowed()) return false
        val params = buildPipParams() ?: return false
        enterPictureInPictureMode(params)
        return true
    }

    /**
     * Checks whether PiP should be offered: active call phase + system feature + user permission.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun isPipAllowed(): Boolean {
        val callState = viewModel.callUiState.value
        if (callState.phase !in PIP_PHASES) return false
        val isLocalVideoOn = callState.localParticipant?.isVideoEnabled == true
        if (callState.call?.isGroupCall == true) {
            // Group call: only allow PIP when local video is on (shows self video in PIP)
            if (!isLocalVideoOn) return false
        } else {
            // P2P call: only allow PIP when at least one side has video
            val isRemoteVideoOn = callState.remoteParticipant?.isVideoEnabled == true
            if (!isLocalVideoOn && !isRemoteVideoOn) return false
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return false
        return hasPipPermission()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("DEPRECATION")
    private fun hasPipPermission(): Boolean {
        return try {
            val appOps = getSystemService(APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    android.os.Process.myUid(),
                    packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    android.os.Process.myUid(),
                    packageName
                )
            }
            result == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(): PictureInPictureParams? = try {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(9, 16))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val enterEnabled = isPipAllowed()
            builder.setAutoEnterEnabled(enterEnabled)
            builder.setSeamlessResizeEnabled(false)
        }
        builder.build()
    } catch (_: Exception) {
        null
    }

    /** Rebuilds and applies PiP params (keeps S+ auto-enter in sync with call state). */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePipParams() {
        buildPipParams()?.let { setPictureInPictureParams(it) }
    }

    private fun answerWithPermissionsIfNeeded() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val isVideoCall = viewModel.callUiState.value.call?.isVideoCall == true
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val permissionsNeeded = buildList {
            if (!hasMicPermission) add(Manifest.permission.RECORD_AUDIO)
            if (isVideoCall && !hasCameraPermission) add(Manifest.permission.CAMERA)
        }

        if (permissionsNeeded.isEmpty()) {
            viewModel.onAnswerClick()
        } else {
            autoAnswerPermissionsLauncher.launch(permissionsNeeded.toTypedArray())
        }
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

    override fun onStop() {
        super.onStop()
        // Don't disable camera while in PiP — the call is still visible
        if (!isInPictureInPictureMode) {
            viewModel.onAppBackground()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForeground()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Worker will stop itself when call ends
    }

    companion object {
        private const val EXTRA_CALL_TYPE = "extra_call_type"
        private const val CALL_TYPE_INCOMING = "incoming"
        private const val EXTRA_AUTO_ANSWER = "extra_auto_answer"

        private val PIP_PHASES = setOf(
            CallUiState.CallPhase.Incoming,
            CallUiState.CallPhase.Outgoing,
            CallUiState.CallPhase.Connecting,
            CallUiState.CallPhase.Connected,
            CallUiState.CallPhase.Reconnecting
        )

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
         * Creates intent that launches the incoming call screen and immediately answers the call.
         * Used by the notification "Answer" action — getActivity() is required since background
         * activity starts from BroadcastReceiver are blocked on Android 10+.
         */
        fun createAnswerIntent(context: Context): Intent {
            return createIncomingIntent(context).apply {
                putExtra(EXTRA_AUTO_ANSWER, true)
            }
        }

        /**
         * Creates intent to return to ongoing call.
         */
        fun createOngoingIntent(context: Context): Intent {
            return Intent(context, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        /**
         * Launches incoming call UI.
         */
        fun launchIncoming(context: Context) {
            val intent = createIncomingIntent(context)
            context.startActivity(intent)
        }

        /**
         * Launches ongoing call UI.
         */
        fun launchOngoing(context: Context) {
            context.startActivity(createOngoingIntent(context))
        }
    }
}
