package com.sceyt.chatuikit.extensions

import android.app.Activity
import android.app.Activity.OVERRIDE_TRANSITION_OPEN
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.view.WindowInsetsControllerCompat
import com.sceyt.chatuikit.SceytChatUIKit

@Suppress("DEPRECATION")
fun Activity.overrideTransitions(enterAnim: Int, exitAnim: Int, isOpen: Boolean) {
    if (SDK_INT >= 34) {
        val type = if (isOpen) OVERRIDE_TRANSITION_OPEN else Activity.OVERRIDE_TRANSITION_CLOSE
        overrideActivityTransition(type, enterAnim, exitAnim)
    } else
        overridePendingTransition(enterAnim, exitAnim)
}

@Suppress("DEPRECATION")
fun Activity.statusBarIconsColorWithBackground(
        isDark: Boolean = isNightMode(),
        @ColorRes statusBarColor: Int = SceytChatUIKit.theme.colors.statusBarColor,
        @ColorRes navigationBarColor: Int = SceytChatUIKit.theme.colors.primaryColor
) {
    // For API 35+, edge-to-edge is enforced and status bar color is deprecated
    // Only set colors for versions before API 35
    if (SDK_INT < 35) {
        window.statusBarColor = getCompatColor(statusBarColor)
        if (isDark)
            window.navigationBarColor = getCompatColor(navigationBarColor)
    } else {
        // For API 35+, use transparent bars as required by edge-to-edge
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    // Set icon colors for all versions
    if (SDK_INT >= M) {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (isDark) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
            // Also set navigation bar icons
            window.insetsController?.setSystemBarsAppearance(
                if (isDark) 0 else WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            val wic = WindowInsetsControllerCompat(window, window.decorView)
            wic.isAppearanceLightStatusBars = !isDark
            if (SDK_INT >= Build.VERSION_CODES.O) {
                wic.isAppearanceLightNavigationBars = !isDark
            }
        }
    }
}

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
    } catch (ex: Exception) {
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