package com.sceyt.chatuikit.extensions

import android.app.Activity
import android.app.Activity.OVERRIDE_TRANSITION_OPEN
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.view.WindowCompat
import com.sceyt.chatuikit.SceytChatUIKit

@Suppress("DEPRECATION")
fun Activity.overrideTransitions(enterAnim: Int, exitAnim: Int, isOpen: Boolean) {
    if (SDK_INT >= 34) {
        val type = if (isOpen) OVERRIDE_TRANSITION_OPEN else Activity.OVERRIDE_TRANSITION_CLOSE
        overrideActivityTransition(type, enterAnim, exitAnim)
    } else
        overridePendingTransition(enterAnim, exitAnim)
}

fun ComponentActivity.applySystemBarsStyle(
    isDarkMode: Boolean = isNightMode(),
    @ColorRes statusBarColor: Int = SceytChatUIKit.theme.colors.statusBarColor,
    @ColorRes navigationBarColor: Int = SceytChatUIKit.theme.colors.primaryColor
) {
    val statusBarColorInt = getCompatColorForNightMode(statusBarColor, isDarkMode)
    val navigationBarColorInt = getCompatColorForNightMode(navigationBarColor, isDarkMode)

    enableEdgeToEdge(
        statusBarStyle = systemBarStyle(isDarkMode, statusBarColorInt),
        navigationBarStyle = navigationBarStyle(isDarkMode, navigationBarColorInt)
    )

    WindowCompat.getInsetsController(window, window.decorView).run {
        isAppearanceLightStatusBars = !isDarkMode
        isAppearanceLightNavigationBars = !isDarkMode
    }
}

private fun systemBarStyle(isDark: Boolean, @ColorInt color: Int): SystemBarStyle {
    return if (isDark) SystemBarStyle.dark(color)
    else SystemBarStyle.light(color, color)
}

private fun navigationBarStyle(isDark: Boolean, @ColorInt color: Int): SystemBarStyle {
    return if (isDark) SystemBarStyle.dark(color)
    else SystemBarStyle.auto(
        lightScrim = defaultLightNavigationBarScrim,
        darkScrim = defaultDarkNavigationBarScrim,
        detectDarkMode = { _ -> false }
    )
}

// Matches AndroidX enableEdgeToEdge default navigation scrims for contrast fallback.
private val defaultLightNavigationBarScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val defaultDarkNavigationBarScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

fun Activity.isKeyboardOpen(): Boolean {
    val rootView = findViewById<View>(android.R.id.content)
    val heightDiff3: Int = getContentView().rootView.height - rootView.height
    return (heightDiff3 > dpToPx(200f))
}

fun Activity.customToastSnackBar(message: String?) {
    try {
        findViewById<View>(android.R.id.content)?.let {
            customToastSnackBar(it, message)
        }
    } catch (_: Exception) {
        if (!isFinishing)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

fun Activity.getContentView() = findViewById<View>(android.R.id.content)

fun Activity.hideSoftInput() {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = currentFocus
    if (view == null) {
        view = View(this)
    }
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Activity.darkModeContext(): Context {
    // Copy current configuration and force night mode
    val nightConfig = Configuration(resources.configuration).apply {
        uiMode = Configuration.UI_MODE_NIGHT_YES
    }

    return ContextThemeWrapper(this, theme).apply {
        applyOverrideConfiguration(nightConfig)
    }
}

fun Context.isActivityFinishingOrDestroying(): Boolean {
    val activity = maybeComponentActivity() ?: return false
    return activity.isFinishing || activity.isDestroyed
}