package com.sceyt.chat.ui.data

interface SceytSharedPreference {
    fun setUsername(userName: String?)
    fun getUsername(): String?
    fun deleteUsername()
    fun setToken(token: String?)
    fun getToken(): String?
    fun clear()
}