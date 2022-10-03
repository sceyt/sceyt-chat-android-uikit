package com.sceyt.sceytchatuikit.data

import android.app.Application
import android.content.Context

class SceytSharedPreferenceImpl(application: Application) : SceytSharedPreference {
    private companion object {
        private const val PREF_NAME = "sceyt_ui_kit_preferences"
        private const val PREF_USER_ID = "user_id"
    }

    private val pref = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun setUserId(id: String?) {
        val editor = pref.edit()
        editor.putString(PREF_USER_ID, id)
        editor.apply()
    }

    override fun getUserId(): String? {
        return pref.getString(PREF_USER_ID, "")
    }

    override fun deleteUsername() {
        val editor = pref.edit()
        editor.remove(PREF_USER_ID)
        editor.apply()
    }

    override fun clear() {
        val editor = pref.edit()
        editor.clear()
        editor.apply()
    }
}