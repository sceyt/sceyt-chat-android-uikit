package com.sceyt.chatuikit.persistence.repositories

interface SceytSharedPreference {
    fun getInt(key: String, defaultValue: Int): Int
    fun setInt(key: String, value: Int)
    fun getString(key: String): String?
    fun setString(key: String, value: String?)
    fun getBoolean(key: String): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun clear()
}