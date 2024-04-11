package com.sceyt.chatuikit.data

interface SceytSharedPreference {
    fun setUserId(id: String?)
    fun getUserId(): String?
    fun getString(key: String): String?
    fun setString(key: String, value: String?)
    fun getBoolean(key: String): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun deleteUsername()
    fun clear()
}