package com.sceyt.chat.ui.data

import android.app.Application
import android.content.Context

class SceytSharedPreferenceImpl(application: Application) : SceytSharedPreference {
    private companion object {
        private const val PREF_NAME = "sceyt_preferences"
        const val PREF_USER_NAME = "username"
        const val PREF_USER_TOKEN = "token"
    }

    private val pref = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun setUsername(userName: String?) {
        val editor = pref.edit()
        editor.putString(PREF_USER_NAME, userName)
        editor.apply()
    }

    override fun getUsername(): String? {
        return pref.getString(PREF_USER_NAME, "")
    }

    override fun deleteUsername() {
        val editor = pref.edit()
        editor.remove(PREF_USER_NAME)
        editor.apply()
    }

    override fun setToken(token: String?) {
        val editor = pref.edit()
        editor.putString(PREF_USER_TOKEN, token)
        editor.apply()
    }

    override fun getToken(): String? {
        return pref.getString(PREF_USER_TOKEN, "")
    }

    override fun clear() {
        val editor = pref.edit()
        editor.clear()
        editor.apply()
    }
}