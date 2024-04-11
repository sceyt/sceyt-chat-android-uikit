package com.sceyt.chatuikit.data

import android.content.Context

internal class SceytSharedPreferenceImpl(context: Context) : SceytSharedPreference {
    private val editor by lazy { pref.edit() }

    companion object {
        private const val PREF_NAME = "sceyt_ui_kit_preferences"
        private const val PREF_USER_ID = "user_id"
        const val KEY_FCM_TOKEN = "key_fcm_token"
        const val KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION = "key_subscribed_for_push"
    }

    private val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun setUserId(id: String?) {
        editor.putString(PREF_USER_ID, id)
        editor.apply()
    }

    override fun getUserId(): String? = pref.getString(PREF_USER_ID, null)

    override fun getString(key: String): String? = pref.getString(key, null)

    override fun getBoolean(key: String): Boolean = pref.getBoolean(key, false)

    override fun setString(key: String, value: String?) {
        editor.putString(key, value)
        editor.apply()
    }

    override fun setBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value)
        editor.apply()
    }

    override fun deleteUsername() {
        editor.remove(PREF_USER_ID)
        editor.apply()
    }

    override fun clear() {
        editor.clear()
        editor.apply()
    }
}