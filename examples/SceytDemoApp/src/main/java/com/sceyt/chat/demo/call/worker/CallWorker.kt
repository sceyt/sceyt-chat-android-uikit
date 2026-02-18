package com.sceyt.chat.demo.call.worker

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.isActive
import com.sceyt.chat.demo.call.notification.CallNotificationChannels
import com.sceyt.chat.demo.call.notification.CallNotificationManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.takeWhile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * WorkManager worker for maintaining call foreground execution.
 * Uses setForeground() to show persistent notification during active calls.
 */
class CallWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    companion object {
        private const val TAG = "CallWorker"
        private const val CALL_WORK_NAME = "sceyt_call_work"

        /**
         * Starts the call worker to maintain foreground execution.
         */
        fun start(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<CallWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    CALL_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )

            Log.d(TAG, "Call worker started")
        }

        /**
         * Stops the call worker.
         */
        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(CALL_WORK_NAME)
            Log.d(TAG, "Call worker stopped")
        }
    }

    private val callManager: CallManager by inject()
    private val notificationManager = CallNotificationManager(context)
    private val systemNotificationManager = context.getSystemService<NotificationManager>()

    override suspend fun doWork(): Result {
        Log.d(TAG, "Call worker doWork started")

        try {
            // Create initial foreground notification
            setForeground(createForegroundInfo())

            // Observe call state and update notification until call ends
            combine(
                callManager.callUiState,
                callManager.mediaState,
                callManager.callDuration,
                callManager.remoteParticipant
            ) { state, media, duration, remote ->
                CallStateSnapshot(state, media.isMuted, duration, remote?.name ?: "Unknown")
            }
                .takeWhile { it.state.isActive }
                .collectLatest { snapshot ->
                    updateNotification(snapshot)
                }

            Log.d(TAG, "Call worker completed - call ended")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Call worker error", e)
            return Result.failure()
        } finally {
            // Clean up notifications
            systemNotificationManager?.cancel(CallNotificationChannels.INCOMING_CALL_NOTIFICATION_ID)
            systemNotificationManager?.cancel(CallNotificationChannels.ONGOING_CALL_NOTIFICATION_ID)
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val state = callManager.callUiState.value
        val notification = buildNotificationForState(state)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                getNotificationId(state),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            ForegroundInfo(getNotificationId(state), notification)
        }
    }

    private var lastNotificationId: Int = -1

    private fun updateNotification(snapshot: CallStateSnapshot) {
        val newNotificationId = getNotificationId(snapshot.state)

        // Cancel old notification if ID changed (e.g., from incoming to ongoing)
        if (lastNotificationId != -1 && lastNotificationId != newNotificationId) {
            systemNotificationManager?.cancel(lastNotificationId)
            Log.d(TAG, "Cancelled old notification: $lastNotificationId, new: $newNotificationId")
        }

        val notification = buildNotificationForState(
            state = snapshot.state,
            remoteName = snapshot.remoteName,
            isMuted = snapshot.isMuted,
            duration = formatDuration(snapshot.duration)
        )

        systemNotificationManager?.notify(newNotificationId, notification)
        lastNotificationId = newNotificationId

        Log.d(TAG, "Updated notification for state: ${snapshot.state::class.simpleName}")
    }

    private fun buildNotificationForState(
        state: CallUiState,
        remoteName: String = "Unknown",
        isMuted: Boolean = false,
        duration: String = "00:00"
    ): android.app.Notification {
        return when (state) {
            is CallUiState.Incoming -> {
                notificationManager.buildIncomingCallNotification(
                    callerName = state.callerName ?: state.callerId,
                    isVideo = state.isVideo,
                    callId = state.call.id
                )
            }
            is CallUiState.Connected -> {
                notificationManager.buildOngoingCallNotification(
                    remoteName = remoteName,
                    duration = duration,
                    isMuted = isMuted,
                    isVideo = false // TODO: get from state
                )
            }
            is CallUiState.Outgoing,
            is CallUiState.Connecting,
            is CallUiState.Reconnecting -> {
                notificationManager.buildConnectingNotification(
                    remoteName = remoteName,
                    state = state
                )
            }
            else -> {
                // For ended/idle states, build a basic notification
                notificationManager.buildConnectingNotification(
                    remoteName = remoteName,
                    state = state
                )
            }
        }
    }

    private fun getNotificationId(state: CallUiState): Int {
        return when (state) {
            is CallUiState.Incoming -> CallNotificationChannels.INCOMING_CALL_NOTIFICATION_ID
            else -> CallNotificationChannels.ONGOING_CALL_NOTIFICATION_ID
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    /**
     * Snapshot of call state for notification updates.
     */
    private data class CallStateSnapshot(
        val state: CallUiState,
        val isMuted: Boolean,
        val duration: Long,
        val remoteName: String
    )
}
