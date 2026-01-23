package com.sceyt.chatuikit.persistence.repositories

interface SceytSharedPreference {
    fun getInt(key: String, defaultValue: Int): Int
    fun setInt(key: String, value: Int)
    fun getString(key: String, defaultValue: String? = null): String?
    fun setString(key: String, value: String?)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun clear()
}