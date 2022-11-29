package com.sceyt.sceytchatuikit.extensions

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.sceyt.sceytchatuikit.R
import java.io.File

fun Any?.isNull() = this == null

fun Any?.isNotNull() = this != null

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

fun customToastSnackBar(view: View?, message: String) {
    try {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    } catch (ex: Exception) {
        Toast.makeText(view?.context, message, Toast.LENGTH_SHORT).show()
    }
}

fun Activity.customToastSnackBar(message: String?) {
    try {
        findViewById<View>(android.R.id.content)?.let {
            message?.let { it1 -> Snackbar.make(it, it1, Snackbar.LENGTH_LONG).show() }
        }
    } catch (ex: Exception) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
    overridePendingTransition(0, 0)
}

fun Activity.statusBarIconsColorWithBackground(isDark: Boolean) {
    window.statusBarColor = getCompatColorByTheme(R.color.sceyt_color_status_bar, isDark)

    if (Build.VERSION.SDK_INT >= M) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (isDark) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= M) {
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
