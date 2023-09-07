package com.sceyt.chat.demo.data

interface AppSharedPreference {
    fun setUserId(userName: String?)
    fun getUserId(): String?
    fun deleteUsername()
    fun setToken(token: String?)
    fun getToken(): String?
    fun clear()
}