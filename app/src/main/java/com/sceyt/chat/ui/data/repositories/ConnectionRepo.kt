package com.sceyt.chat.ui.data.repositories

import com.sceyt.chat.ui.data.api.AuthApiService
import com.sceyt.chat.ui.data.makeApiCall

class ConnectionRepo(private val authApiService: AuthApiService) {

    suspend fun getSceytToken(userId: String) = makeApiCall {
        authApiService.getSceytToken(userId)
    }
}