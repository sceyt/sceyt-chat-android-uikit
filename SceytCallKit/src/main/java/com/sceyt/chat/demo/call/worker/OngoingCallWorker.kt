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
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.manager.CallManagerImpl
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.manager.CallUiState.CallPhase
import com.sceyt.chat.demo.call.notification.CallNotificationChannels
import com.sceyt.chat.demo.call.notification.CallNotificationManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.takeWhile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

/**
 * WorkManager worker that maintains a foreground service during outgoing and active calls.
 * Uses FOREGROUND_SERVICE_TYPE_PHONE_CALL | FOREGROUND_SERVICE_TYPE_MICROPHONE because
 * the microphone is in use for both outgoing (initiating) and answered incoming calls.
 * Started from [CallManagerImpl.startOutgoingCall] and [CallManagerImpl.answerIncomingCall].
 */
class OngoingCallWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    companion object {
        private const val TAG = "OutgoingCallWorker"
        const val OUTGOING_CALL_WORK_NAME = "sceyt_outgoing_call_work"

        fun start(context: Context): Operation {
            val request = OneTimeWorkRequestBuilder<OngoingCallWorker>()
                .addTag(OUTGOING_CALL_WORK_NAME)
                .build()
            Log.d(TAG, "OutgoingCallWorker started")
            return WorkManager.getInstance(context)
                .enqueueUniqueWork(OUTGOING_CALL_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(OUTGOING_CALL_WORK_NAME)
            Log.d(TAG, "OutgoingCallWorker stopped")
        }
    }

    private val callManager: CallManager by inject()
    private val notificationManager = CallNotificationManager(applicationContext)
    private val systemNotificationManager = applicationContext.getSystemService<NotificationManager>()

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork started")
        try {
            setForeground(createForegroundInfo())

            combine(
                callManager.callUiState,
                callManager.mediaState,
                callManager.callDuration,
            ) { state, media, duration ->
                Triple(state, media, duration)
            }
                .takeWhile { (state, _, _) -> state.isActive }
                .collectLatest { (state, media, duration) ->
                    val notification = buildNotificationForState(
                        state = state,
                        isMuted = media.isMuted,
                        duration = formatDuration(duration)
                    )
                    systemNotificationManager?.notify(
                        CallNotificationChannels.CALL_NOTIFICATION_ID,
                        notification
                    )
                }

            Log.d(TAG, "doWork completed — call ended")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork error", e)
            return Result.failure()
        } finally {
            systemNotificationManager?.cancel(CallNotificationChannels.CALL_NOTIFICATION_ID)
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = buildNotificationForState(callManager.callUiState.value)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            ForegroundInfo(CallNotificationChannels.CALL_NOTIFICATION_ID, notification, type)
        } else {
            ForegroundInfo(CallNotificationChannels.CALL_NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotificationForState(
        state: CallUiState,
        isMuted: Boolean = false,
        duration: String = "00:00"
    ): android.app.Notification {
        val remoteName = state.remoteUserName ?: state.remoteUserId
        return when (state.phase) {
            CallPhase.Connected ->
                notificationManager.buildOngoingCallNotification(remoteName, duration, isMuted, state.isVideo)
            else ->
                notificationManager.buildConnectingNotification(remoteName, state)
        }
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}
