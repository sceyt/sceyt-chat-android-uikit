package com.sceyt.chat.ui.extencions

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

fun Context.getCompatColor(@ColorRes colorId: Int) = ContextCompat.getColor(this, colorId)

fun Context.getCompatColorNight(@ColorRes colorId: Int): Int {
    val res = resources
    val configuration = Configuration(res.configuration)
    configuration.uiMode = Configuration.UI_MODE_NIGHT_YES
    return createConfigurationContext(configuration).getCompatColor(colorId)
}

fun Context.getCompatColorByTheme(@ColorRes colorId: Int, isDark: Boolean): Int {
    val res = resources
    val configuration = Configuration(res.configuration)
    configuration.uiMode = if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    return createConfigurationContext(configuration).getCompatColor(colorId)
}

fun Context.getCompatDrawable(@DrawableRes drawableId: Int) = ContextCompat.getDrawable(this, drawableId)


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

    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(editText, 0)
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




