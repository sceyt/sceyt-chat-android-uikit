package com.sceyt.chat.demo.data.repositories

import com.sceyt.chat.demo.data.api.AuthApiService
import com.sceyt.chat.demo.data.makeApiCall

class ConnectionRepo(
        private val authApiService: AuthApiService
) {

    suspend fun getSceytToken(userId: String) = makeApiCall {
        authApiService.getSceytToken(userId)
    }
}