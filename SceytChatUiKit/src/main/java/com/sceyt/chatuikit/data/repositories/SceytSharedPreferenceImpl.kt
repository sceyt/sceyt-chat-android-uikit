package com.sceyt.chatuikit.data.repositories

import android.content.Context
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference

internal object Keys {
    const val KEY_USER_ID = "user_id"
}

internal fun SceytSharedPreference.getUserId(): String? = getString(Keys.KEY_USER_ID)

internal class SceytSharedPreferenceImpl(context: Context) : SceytSharedPreference {
    private val editor by lazy { pref.edit() }

    companion object {
        private const val PREF_NAME = "sceyt_ui_kit_preferences"
    }

    private val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = pref.getString(key, null)

    override fun setInt(key: String, value: Int) {
        editor.putInt(key, value)
        editor.apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int = pref.getInt(key, defaultValue)

    override fun getBoolean(key: String): Boolean = pref.getBoolean(key, false)

    override fun setString(key: String, value: String?) {
        editor.putString(key, value)
        editor.apply()
    }

    override fun setBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value)
        editor.apply()
    }

    override fun clear() {
        editor.clear()
        editor.apply()
    }
}