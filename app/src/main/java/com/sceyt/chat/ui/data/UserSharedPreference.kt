package com.sceyt.chat.ui.data

import android.content.Context
import android.content.SharedPreferences

object UserSharedPreference {
    private const val PREF_NAME = "sceyt_preferences"
    const val PREF_USER_NAME = "username"
    const val PREF_USER_TOKEN = "token"

    fun getSharedPreferences(ctx: Context): SharedPreferences {
        return ctx.getPref()
    }

    fun setUsername(ctx: Context, userName: String?) {
        val editor = ctx.getPref().edit()
        editor.putString(PREF_USER_NAME, userName)
        editor.apply()
    }

    fun getUsername(ctx: Context): String? {
        return ctx.getPref().getString(PREF_USER_NAME, "")
    }

    fun deleteUsername(ctx: Context) {
        val editor = ctx.getPref().edit()
        editor.remove(PREF_USER_NAME)
        editor.apply()
    }

    fun setToken(ctx: Context, token: String?) {
        val editor = ctx.getPref().edit()
        editor.putString(PREF_USER_TOKEN, token)
        editor.apply()
    }

    fun getToken(ctx: Context): String? {
        return ctx.getPref().getString(PREF_USER_TOKEN, "")
    }

    private fun Context.getPref(): SharedPreferences {
        return getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}