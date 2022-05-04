package com.sceyt.chat.ui.extensions

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.forEach
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.sceyt.chat.ui.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

fun Any?.isNull() = this == null

fun Any?.isNotNull() = this != null

fun BottomNavigationView.disableTooltip() {
    val content: View = getChildAt(0)
    if (content is ViewGroup) {
        content.forEach {
            it.setOnLongClickListener {
                return@setOnLongClickListener true
            }
        }
    }
}

inline fun <reified T> checkIsOtherAndCloseDialog(dialog: Dialog?): Boolean {
    return if (dialog !is T) {
        dialog?.dismiss()
        true
    } else false
}

inline fun <reified T> checkIsSomeDialog(dialog: Dialog?): Boolean {
    return dialog is T
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

fun DialogFragment.setBundleArguments(init: Bundle.() -> Unit = {}): DialogFragment {
    arguments = Bundle().apply { init() }
    return this
}

inline fun <reified T : DialogFragment> DialogFragment.setBundleArgumentsTyped(init: Bundle.() -> Unit = {}): T {
    arguments = Bundle().apply { init() }
    return this as T
}

inline fun <reified T : Any> newIntent(context: Context): Intent = Intent(context, T::class.java)

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus)
}

fun Activity.hideKeyboard(view: View?) {

    val baseView = this.findViewById<View>(android.R.id.content)
    view?.clearFocus()
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view?.windowToken ?: baseView.windowToken, 0)

}


fun Activity.postDelayed(delayInMillis: Long, functionToExecute: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed({
        if (isFinishing.not()) {
            functionToExecute.invoke()
        }
    }, delayInMillis)
}


fun Activity.setLightStatusBar(view: View?) {
    if (Build.VERSION.SDK_INT >= M && view != null) {
        var flags = view.systemUiVisibility
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        view.systemUiVisibility = flags
        window.statusBarColor = Color.WHITE
    }
}

fun Activity.setStatusBarTitleDarOrLight(isLight: Boolean) {
    if (Build.VERSION.SDK_INT >= M) {
        // window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (isLight)
            window.decorView.systemUiVisibility = android.R.attr.windowLightStatusBar
        else
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        //     window.statusBarColor = if (isLight) Color.TRANSPARENT else ContextCompat.getColor(this, R.color.status_bar_color)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
}

fun Activity.clearLightStatusBar() {
    if (Build.VERSION.SDK_INT >= M) {
        window.statusBarColor = Color.TRANSPARENT
    }
}

fun AppCompatActivity.finishDelayed(duration: Long = 200, resultIntent: Intent.() -> Unit = {}, startFinish: (() -> Unit?)? = null) {
    lifecycleScope.launch {
        delay(duration)
        startFinish?.invoke()
        setResult(Activity.RESULT_OK, Intent().apply(resultIntent))
        finish()
    }
}

fun DialogFragment.dismissSafety() {
    try {
        dismissAllowingStateLoss()
    } catch (ex: Exception) {
    }
}

fun DialogFragment?.isNullOrNotAdded(): Boolean {
    return this == null || !this.isAdded
}

fun Dialog.dismissSafety() {
    try {
        if (isShowing)
            dismiss()
    } catch (ex: Exception) {
    }
}

fun Dialog.showSafety() {
    try {
        show()
    } catch (ex: Exception) {
    }
}

fun Dialog.checkAndShowSafety() {
    try {
        if (isShowing)
            return
        showSafety()
    } catch (ex: Exception) {
    }
}

fun Dialog.checkAndDismissSafety() {
    try {
        if (!isShowing)
            return
        dismissSafety()
    } catch (ex: Exception) {
    }
}

fun Dialog?.isNullOrNotShowing(): Boolean {
    return if (this == null)
        return true
    else !isShowing
}


fun Dialog?.isShowing(): Boolean {
    return this?.isShowing ?: return false
}

fun checkIfImagePathIsLocale(path: String?): Boolean {
    if (path != null) {
        val file = File(path)
        return file.exists()
    }
    return false
}


fun Activity.getRootView() = findViewById<View>(android.R.id.content)

fun Activity.recreateWithoutAnim() {
    finish()
    startActivity(intent)
    overridePendingTransition(0, 0)
}

fun Long.convertMSIntoHourMinSeconds(): String {
    return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(this),
        TimeUnit.MILLISECONDS.toMinutes(this) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(this)),
        TimeUnit.MILLISECONDS.toSeconds(this) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this)))
}

fun Activity.statusBarIconsColorWithBackground(isDark: Boolean) {
    if (Build.VERSION.SDK_INT >= M) {
        window.statusBarColor = getCompatColorByTheme(R.color.colorPrimary, isDark)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (isDark) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= M) {
            val wic = WindowInsetsControllerCompat(window, window.decorView)
            wic.isAppearanceLightStatusBars = !isDark
        }
    } else {
        val color = if (isDark) getCompatColorByTheme(R.color.colorPrimary, true)
        else getCompatColor(R.color.colorPrimaryDark)
        window.statusBarColor = color
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
