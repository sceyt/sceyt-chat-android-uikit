package com.sceyt.sceytchatuikit.data

internal interface SceytSharedPreference {
    fun setUserId(id: String?)
    fun getUserId(): String?
    fun deleteUsername()
    fun clear()
}