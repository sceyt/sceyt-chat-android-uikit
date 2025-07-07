package com.sceyt.chatuikit.extensions

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.PowerManager
import android.view.View
import android.widget.Toast
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.material.snackbar.Snackbar
import java.io.Serializable
import kotlin.math.min

fun Any?.isNull() = this == null

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? {
    return IntentCompat.getParcelableExtra(this, key, T::class.java)
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? {
    return BundleCompat.getParcelable(this, key, T::class.java)
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? {
    return BundleCompat.getSerializable(this, key, T::class.java)
}

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? {
    return BundleCompat.getParcelableArrayList(this, key, T::class.java)
}

inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? {
    return IntentCompat.getParcelableArrayListExtra(this, key, T::class.java)
}

fun Context.isAppOnForeground(): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val appProcesses = activityManager.runningAppProcesses ?: return false
    val packageName = applicationContext.packageName
    appProcesses.forEach {
        if (it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && it.processName == packageName) {
            return true
        }
    }
    return false
}

fun isAppOnForeground(): Boolean {
    return ProcessLifecycleOwner.get().isResumed()
}

fun Context.isAppInDarkMode(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun Context.getOrientation(): Int {
    return resources.configuration.orientation
}

fun Context.screenWidthPx() = resources.displayMetrics.widthPixels

fun Context.screenHeightPx() = resources.displayMetrics.heightPixels

fun Context.screenPortraitWidthPx() = min(screenWidthPx(), screenHeightPx())

fun Fragment.screenHeightPx() = resources.displayMetrics.heightPixels

fun customToastSnackBar(view: View?, message: String?, maxLines: Int = 5) {
    try {
        if (view != null && !message.isNullOrBlank())
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setTextMaxLines(maxLines)
                .show()
    } catch (ex: Exception) {
        view?.context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
    }
}

fun runOnMainThread(run: () -> Unit) {
    Handler(Looper.getMainLooper()).post(run)
}

fun Context.isRtl() = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

fun Context.isLandscape(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

@Suppress("DEPRECATION")
fun Context.keepScreenOn(): PowerManager.WakeLock {
    return (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
        newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "myApp:proximity_wakelock").apply {
            acquire(10 * 60 * 1000L /*10 minutes*/)
        }
    }
}

fun LifecycleOwner.isResumed() = lifecycle.currentState == Lifecycle.State.RESUMED

inline fun doSafe(action: () -> Unit) {
    try {
        action()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}