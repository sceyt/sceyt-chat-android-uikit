package com.sceyt.chat.demo.data

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sceyt.chat.demo.data.AppSharedPreference.Companion.PREF_USER_ID

class AppSharedPreferenceImpl(application: Application) : AppSharedPreference {

    companion object {
        private const val PREF_NAME = "simple_preferences"
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

    override fun <T> getList(key: String, clazz: Class<T>): List<T>? = runCatching {
        val typeOfT = TypeToken.getParameterized(List::class.java, clazz).type
        Gson().fromJson<List<T>>(getString(key), typeOfT)
    }.getOrNull()

    override fun <T> putList(key: String, list: List<T>?) {
        runCatching {
            val gson = Gson()
            val json = gson.toJson(list)
            setString(key, json)
        }
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