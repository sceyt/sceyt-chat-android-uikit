package com.sceyt.chat.ui.data

interface AppSharedPreference {
    fun setUsername(userName: String?)
    fun getUsername(): String?
    fun deleteUsername()
    fun setToken(token: String?)
    fun getToken(): String?
    fun clear()
}