package com.sceyt.chat.demo.call.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.core.content.getSystemService

internal class CallProximityWakeLockController(context: Context) {
    private val powerManager = context.applicationContext.getSystemService<PowerManager>()
    private val wakeLock = powerManager
        ?.takeIf { it.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) }
        ?.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, WAKE_LOCK_TAG)
        ?.apply { setReferenceCounted(false) }

    fun setEnabled(enabled: Boolean) {
        if (enabled) acquire() else release()
    }

    @SuppressLint("WakelockTimeout")
    private fun acquire() {
        val lock = wakeLock ?: return
        if (lock.isHeld) return

        try {
            lock.acquire()
            Log.d(TAG, "Proximity wake lock acquired")
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to acquire proximity wake lock", e)
        }
    }

    fun release() {
        val lock = wakeLock ?: return
        if (!lock.isHeld) return

        try {
            lock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
            Log.d(TAG, "Proximity wake lock released")
        } catch (e: RuntimeException) {
            Log.w(TAG, "Unable to release proximity wake lock", e)
        }
    }

    private companion object {
        private const val TAG = "CallProximity"
        private const val WAKE_LOCK_TAG = "SceytCallKit:proximity"
    }
}
