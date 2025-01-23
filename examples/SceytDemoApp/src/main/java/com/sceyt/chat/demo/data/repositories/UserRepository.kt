package com.sceyt.chat.demo.data.repositories

import com.sceyt.chat.demo.data.api.UserApiService
import com.sceyt.chat.demo.data.makeApiCall

class UserRepository(
        private val userApiService: UserApiService
) {

    suspend fun checkUsername(username: String) = makeApiCall {
        userApiService.checkUsername(username)
    }

    suspend fun deleteUser(userId: String) = makeApiCall {
        userApiService.deleteUser(userId)
    }
}
