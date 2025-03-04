package com.sceyt.chat.demo.data

interface AppSharedPreference {
    companion object {
        const val PREF_USER_ID = "user_id"
        const val PREF_USER_TOKEN = "token"
        const val PREF_USER_IDS = "user_ids"
    }

    fun deleteUsername()
    fun setString(key: String, value: String?)
    fun getString(key: String): String?
    fun setBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun clear()
    fun <T> getList(key: String, clazz: Class<T>): List<T>?
    fun <T> putList(key: String, list: List<T>?)
}