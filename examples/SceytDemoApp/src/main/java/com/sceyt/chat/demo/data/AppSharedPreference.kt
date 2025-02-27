package com.sceyt.chat.demo.data

interface AppSharedPreference {
    companion object {
        const val PREF_USER_ID = "user_id"
        const val PREF_USER_TOKEN = "token"
    }

    fun deleteUsername()
    fun setString(key: String, value: String?)
    fun getString(key: String): String?
    fun setBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun addUserId(userId: String)
    fun deleteUserId(userId: String)
    fun getUserIdList(): List<String>
    fun clear()
}