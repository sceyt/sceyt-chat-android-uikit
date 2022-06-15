package com.sceyt.chat.ui.extensions

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentManager
import com.sceyt.chat.ui.BuildConfig
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import java.io.File


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

fun Context.getCompatColor(@ColorRes colorId: Int) = ContextCompat.getColor(this, colorId)

fun Context.getCompatColorNight(@ColorRes colorId: Int): Int {
    val res = resources
    val configuration = Configuration(res.configuration)
    configuration.uiMode = Configuration.UI_MODE_NIGHT_YES
    return createConfigurationContext(configuration).getCompatColor(colorId)
}

fun Context.getCompatColorByTheme(@ColorRes colorId: Int?, isDark: Boolean = SceytUIKitConfig.isDarkMode): Int {
    colorId ?: return 0
    val res = resources
    val configuration = Configuration(res.configuration)
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

fun Context.asAppCompatActivity() = (this as AppCompatActivity)

fun Context.getFileUriWithProvider(file: File): Uri {
    return FileProvider.getUriForFile(this,
        BuildConfig.APPLICATION_ID + ".provider", file)
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

private fun Context.isAirplaneModeOn(): Boolean {
    return Settings.System.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
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

fun Context.gpsIsEnabled(): Boolean {
    val locMan = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return if (isAirplaneModeOn()) {
        //airplane is OFF, check Providers
        //NETWORK_PROVIDER disabled, try GPS
        locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)
    } else {
        //airplane mode is ON
        //here you need to check GPS only, as network is OFF for sure
        locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}

fun Context.isNightTheme(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

internal fun Context?.getFragmentManager(): FragmentManager? {
    return when (this) {
        is AppCompatActivity -> supportFragmentManager
        is ContextWrapper -> baseContext.getFragmentManager()
        else -> null
    }
}





