package com.sceyt.sceytchatuikit.data

internal interface SceytSharedPreference {
    fun setUsername(userName: String?)
    fun getUsername(): String?
    fun deleteUsername()
    fun setToken(token: String?)
    fun getToken(): String?
    fun clear()
}