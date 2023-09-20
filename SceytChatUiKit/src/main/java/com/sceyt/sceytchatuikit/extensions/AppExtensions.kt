package com.sceyt.sceytchatuikit.extensions

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.sceyt.sceytchatuikit.R
import java.io.File
import java.io.Serializable

fun Any?.isNull() = this == null

fun Any?.isNotNull() = this != null

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    SDK_INT >= 33 -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
    SDK_INT >= 33 -> getParcelableArrayList(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}

inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
    SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
}

fun Application.isAppOnForeground(): Boolean {
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

fun Context.getOrientation(): Int {
    return resources.configuration.orientation
}

fun Activity.isFinishingOrDestroyed() = isFinishing || isDestroyed

fun Activity.isNotFinishingOrDestroyed() = !isFinishing && !isDestroyed

fun Activity.hideSoftInput() {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = currentFocus
    if (view == null) {
        view = View(this)
    }
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun View.hideSoftInput() {
    val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun Activity.isKeyboardOpen(): Boolean {
    val rootView = findViewById<View>(android.R.id.content)
    val heightDiff3: Int = getRootView().rootView.height - rootView.height
    return (heightDiff3 > dpToPx(200f))
}

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

fun Activity.customToastSnackBar(message: String?) {
    try {
        findViewById<View>(android.R.id.content)?.let {
            customToastSnackBar(it, message)
        }
    } catch (ex: Exception) {
        if (!isFinishing)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

fun Fragment.customToastSnackBar(message: String?) {
    try {
        if (isAdded)
            customToastSnackBar(view, message)
        else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    } catch (ex: Exception) {
        view?.context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
    }
}


fun Fragment.setBundleArguments(init: Bundle.() -> Unit = {}): Fragment {
    arguments = Bundle().apply { init() }
    return this
}


inline fun <reified T : DialogFragment> DialogFragment.setBundleArgumentsTyped(init: Bundle.() -> Unit = {}): T {
    arguments = Bundle().apply { init() }
    return this as T
}

inline fun <reified T : Any> newIntent(context: Context): Intent = Intent(context, T::class.java)

fun Activity.postDelayed(delayInMillis: Long, functionToExecute: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed({
        if (isFinishing.not()) {
            functionToExecute.invoke()
        }
    }, delayInMillis)
}


fun checkIfImagePathIsLocale(path: String?): Boolean {
    if (path != null) {
        val file = File(path)
        return file.exists()
    }
    return false
}

fun runOnMainThread(run: () -> Unit) {
    Handler(Looper.getMainLooper()).post(run)
}

fun Activity.getRootView() = findViewById<View>(android.R.id.content)

fun Activity.recreateWithoutAnim() {
    finish()
    startActivity(intent)
    overrideTransitions(0, 0, false)
}

fun Context.isRtl() = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

fun Context.isLandscape(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

fun Activity.statusBarIconsColorWithBackground(isDark: Boolean,
                                               @ColorRes statusBarColor: Int = R.color.sceyt_color_status_bar,
                                               @ColorRes navigationBarColor: Int = R.color.sceyt_color_status_bar) {

    window.statusBarColor = getCompatColorByTheme(statusBarColor, isDark)
    if (isDark)
        window.navigationBarColor = getCompatColorByTheme(navigationBarColor, true)

    if (SDK_INT >= M) {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (isDark) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (SDK_INT >= M) {
            val wic = WindowInsetsControllerCompat(window, window.decorView)
            wic.isAppearanceLightStatusBars = !isDark
        }
    }
}

fun Activity.statusBarBackgroundColor(color: Int) {
    window.statusBarColor = color
}

inline fun <reified T> Any.castSafety(): T? {
    return if (this is T)
        this else null
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

inline fun activityLifecycleCallbacks(
        crossinline onActivityCreated: (activity: Activity, savedInstanceState: Bundle?) -> Unit = { _, _ -> },
        crossinline onActivityStarted: (activity: Activity) -> Unit = { _ -> },
        crossinline onActivityResumed: (activity: Activity) -> Unit = { _ -> },
        crossinline onActivityPaused: (activity: Activity) -> Unit = { _ -> },
        crossinline onActivityStopped: (activity: Activity) -> Unit = { _ -> },
        crossinline onActivitySaveInstanceState: (activity: Activity, outState: Bundle) -> Unit = { _, _ -> },
        crossinline onActivityDestroyed: (activity: Activity) -> Unit = { _ -> }): Application.ActivityLifecycleCallbacks {
    return object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            onActivityCreated(activity, savedInstanceState)
        }

        override fun onActivityStarted(activity: Activity) {
            onActivityStarted(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            onActivityResumed(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            onActivityPaused(activity)
        }

        override fun onActivityStopped(activity: Activity) {
            onActivityStopped(activity)
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            onActivitySaveInstanceState(activity, outState)
        }

        override fun onActivityDestroyed(activity: Activity) {
            onActivityDestroyed(activity)
        }
    }
}
