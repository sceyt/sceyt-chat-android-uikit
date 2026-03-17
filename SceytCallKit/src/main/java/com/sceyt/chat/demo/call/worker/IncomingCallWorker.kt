package com.sceyt.chat.demo.call.worker

import android.app.Notification
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
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.CallUiState.CallPhase
import com.sceyt.chat.demo.call.manager.displayTitle
import com.sceyt.chat.demo.call.manager.isVideoCall
import com.sceyt.chat.demo.call.notification.CallNotificationChannels
import com.sceyt.chat.demo.call.notification.CallNotificationManager
import com.sceyt.chatuikit.extensions.isAppOnForeground
import kotlinx.coroutines.flow.takeWhile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * WorkManager worker that maintains a foreground service during an incoming call (ringing phase).
 * Uses FOREGROUND_SERVICE_TYPE_PHONE_CALL only — the microphone is not yet in use.
 * Stops automatically when the call phase transitions away from Incoming (answered or declined).
 */
class IncomingCallWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    companion object {
        private const val TAG = "IncomingCallWorker"
        const val INCOMING_CALL_WORK_NAME = "sceyt_incoming_call_work"
        const val KEY_FOREGROUND_READY = "foreground_ready"

        fun start(context: Context): Operation {
            val request = OneTimeWorkRequestBuilder<IncomingCallWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(INCOMING_CALL_WORK_NAME)
                .build()
            Log.d(TAG, "IncomingCallWorker started")
            return WorkManager.getInstance(context)
                .enqueueUniqueWork(INCOMING_CALL_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(INCOMING_CALL_WORK_NAME)
            Log.d(TAG, "IncomingCallWorker stopped")
        }
    }

    private val callManager: CallManager by inject()
    private val notificationManager = CallNotificationManager(applicationContext)
    private val systemNotificationManager = applicationContext.getSystemService<NotificationManager>()

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork started")
        try {
            val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    CallNotificationChannels.CALL_NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                ForegroundInfo(
                    CallNotificationChannels.CALL_NOTIFICATION_ID,
                    buildNotification()
                )
            }
            setForeground(foregroundInfo)
            setProgress(workDataOf(KEY_FOREGROUND_READY to true))

            // Re-notify as caller name/avatar arrive via async user info fetch
            callManager.callUiState
                .takeWhile { it.phase == CallPhase.Incoming }
                .collect { state ->
                    systemNotificationManager?.notify(
                        CallNotificationChannels.CALL_NOTIFICATION_ID,
                        buildNotification(state)
                    )
                }

            Log.d(TAG, "doWork completed — call is no longer Incoming")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork error", e)
            return Result.failure()
        } finally {
            systemNotificationManager?.cancel(CallNotificationChannels.CALL_NOTIFICATION_ID)
        }
    }

    private fun buildNotification(
        state: CallUiState = callManager.callUiState.value
    ): Notification {
        return notificationManager.buildIncomingCallNotification(
            callerName = state.call?.displayTitle(state.participants)
                ?: state.remoteParticipant?.displayName.orEmpty(),
            isVideo = state.call?.isVideoCall == true,
            suppressFullScreenIntent = applicationContext.isAppOnForeground()
        )
    }
}
