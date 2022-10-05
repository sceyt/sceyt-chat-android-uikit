package com.sceyt.sceytchatuikit.extensions

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


fun Context.getCompatColor(@ColorRes colorId: Int) = ContextCompat.getColor(this, colorId)

fun Context.getCompatColorNight(@ColorRes colorId: Int): Int {
    val res = resources
    val configuration = Configuration(res.configuration)
    configuration.uiMode = Configuration.UI_MODE_NIGHT_YES
    return createConfigurationContext(configuration).getCompatColor(colorId)
}

fun Context.getCompatColorByTheme(@ColorRes colorId: Int?, isDark: Boolean = SceytUIKitConfig.isDarkMode): Int {
    colorId ?: return 0
    val configuration = Configuration(resources.configuration)
    configuration.uiMode = if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    return createConfigurationContext(configuration)?.getCompatColor(colorId) ?: 0
}

fun Context.getCompatDrawableByTheme(@DrawableRes drawableId: Int?, isDark: Boolean): Drawable? {
    drawableId ?: return null
    val res = resources
    val configuration = Configuration(res.configuration)
    configuration.uiMode = if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    return createConfigurationContext(configuration)?.getCompatDrawable(drawableId)
}

fun Context.getCompatDrawable(@DrawableRes drawableId: Int) = ContextCompat.getDrawable(this, drawableId)

fun Context.asAppCompatActivity(): AppCompatActivity {
    if (this is AppCompatActivity)
        return this
    else throw RuntimeException("Context should be AppCompatActivity")
}

fun Context.asActivity(): Activity {
    if (this is Activity)
        return this
    else throw RuntimeException("Context should be Activity")
}

fun Context.getFileUriWithProvider(file: File): Uri {
    return FileProvider.getUriForFile(this,
        "$packageName.provider", file)
}

fun Context.shortToast(message: String) {
    toast(message, Toast.LENGTH_SHORT)
}

fun Context.longToast(message: String?) {
    toast(message, Toast.LENGTH_LONG)
}

fun Context.shortToast(message: Int) {
    toast(message, Toast.LENGTH_SHORT)
}

fun Context.longToast(messageResourceId: Int) {
    toast(messageResourceId, Toast.LENGTH_LONG)
}

fun Context.toast(message: String?, length: Int) {
    Toast.makeText(this, message, length).show()
}

fun Context.toast(messageResourceId: Int, length: Int) {
    Toast.makeText(this, messageResourceId, length).show()
}

inline fun <reified T : Any> Context.launchActivity(
        options: Bundle? = null,
        noinline init: Intent.() -> Unit = {},
) {
    val intent = newIntent<T>(this)
    intent.init()
    startActivity(intent, options)
}

fun Context.showSoftInput(editText: EditText) {
    editText.isFocusable = true
    editText.isFocusableInTouchMode = true
    editText.requestFocus()
    var showed = false
    val run = Runnable {
        editText.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        showed = inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }
    run.run()
    if (!showed)
        Handler(Looper.getMainLooper()).postDelayed(run, 200)
}

fun Context.hideKeyboard(view: EditText?) {
    view?.clearFocus()
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)

}

fun Context.setClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = ClipData.newPlainText("Copied Text", text)
    clipboard.setPrimaryClip(clip)
}


fun Context.isNightTheme(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun Context.checkActiveInternetConnection(timeout: Int = 2000): Boolean {
    if (hasActiveNetwork()) {
        try {
            val urlConnection: HttpURLConnection = URL("https://www.google.com").openConnection() as HttpURLConnection
            urlConnection.setRequestProperty("User-Agent", "Test")
            urlConnection.setRequestProperty("Connection", "close")
            urlConnection.connectTimeout = timeout
            urlConnection.connect()
            return urlConnection.responseCode == 200
        } catch (e: IOException) {
            Log.e("internetConnection", e.message.toString())
        }
    }
    return false
}


fun Context.hasActiveNetwork(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val capability = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        capability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    } else {
        connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnected
    }
}

internal fun Context?.getFragmentManager(): FragmentManager? {
    return when (this) {
        is AppCompatActivity -> supportFragmentManager
        is ContextWrapper -> baseContext.getFragmentManager()
        else -> null
    }
}

fun Context.openLink(url: String?) {
    if (url.isNullOrBlank()) return
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URLUtil.guessUrl(url))))
    } catch (ex: Exception) {
    }
}




