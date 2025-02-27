package com.sceyt.chat.demo.data

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.sceyt.chat.demo.data.AppSharedPreference.Companion.PREF_USER_ID

class AppSharedPreferenceImpl(application: Application) : AppSharedPreference {

    companion object {
        private const val PREF_NAME = "simple_preferences"
        private const val PREF_USER_IDS = "user_ids"
    }

    private val pref = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun setString(key: String, value: String?) {
        val editor = pref.edit()
        editor.putString(key, value)
        editor.apply()
    }

    override fun getString(key: String): String? {
        return pref.getString(key, null)
    }

    override fun setBoolean(key: String, value: Boolean) {
        val editor = pref.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return pref.getBoolean(key, defaultValue)
    }

    override fun deleteUsername() {
        val editor = pref.edit()
        editor.remove(PREF_USER_ID)
        editor.apply()
    }

    override fun addUserId(userId: String) {
        val updatedUserIds = listOf(userId).plus(getUserIdList())
        val json = Gson().toJson(updatedUserIds)
        pref.edit { putString(PREF_USER_IDS, json) }
    }

    override fun deleteUserId(userId: String) {
        val updatedUserIds = getUserIdList().filter { it != userId }
        val json = Gson().toJson(updatedUserIds)
        pref.edit { putString(PREF_USER_IDS, json) }
    }

    override fun getUserIdList(): List<String> {
        val json = pref.getString(PREF_USER_IDS, "")
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson<List<String>>(json, type).orEmpty()
    }

    override fun clear() {
        val editor = pref.edit()
        editor.clear()
        editor.apply()
    }

}