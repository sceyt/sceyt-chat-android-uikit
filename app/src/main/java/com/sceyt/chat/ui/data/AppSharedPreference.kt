package com.sceyt.chat.ui.data

interface AppSharedPreference {
    fun setUserName(userName: String?)
    fun getUserName(): String?
    fun deleteUsername()
    fun setToken(token: String?)
    fun getToken(): String?
    fun clear()
}