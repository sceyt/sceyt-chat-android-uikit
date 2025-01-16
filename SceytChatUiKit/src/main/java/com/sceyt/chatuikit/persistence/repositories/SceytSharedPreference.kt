package com.sceyt.chatuikit.persistence.repositories

interface SceytSharedPreference {
    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION = "key_subscribed_for_push"
    }

    fun getInt(key: String, defaultValue: Int): Int
    fun setInt(key: String, value: Int)
    fun getString(key: String): String?
    fun setString(key: String, value: String?)
    fun getBoolean(key: String): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun clear()
}

internal fun SceytSharedPreference.getUserId(): String? = getString(SceytSharedPreference.KEY_USER_ID)