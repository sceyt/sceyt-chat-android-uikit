package com.sceyt.sceytchatuikit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SceytKitClient {

    private fun connect() {
       /* val token = preference.getToken()
        val userName = preference.getUsername()
        if (token.isNullOrBlank()) {
            connectWithoutToken(userName ?: return)
        } else if (!token.isNullOrEmpty())
            connectWithToken(token, userName ?: return)*/
    }

    private fun connectWithToken(token: String, userName: String): LiveData<Boolean> {
        val success: MutableLiveData<Boolean> = MutableLiveData()
       /* chatClient.connect(token)
        addListener(success, token, userName)*/
        return success
    }

    fun connectWithoutToken(username: String): LiveData<Boolean> {
        val success: MutableLiveData<Boolean> = MutableLiveData()
/*
        getTokenByUserName(username, {
            val token = it.get("token")
            chatClient.connect(token as String?)
            addListener(success, token, username)
        }, {
            success.postValue(false)
        }, this)*/

        return success
    }
}