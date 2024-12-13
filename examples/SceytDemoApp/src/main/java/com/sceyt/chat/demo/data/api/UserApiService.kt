package com.sceyt.chat.demo.data.api

import com.sceyt.chat.demo.data.models.DeleteUserResponse
import com.sceyt.chat.demo.data.models.UsernamevalidationResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface UserApiService {

    @GET("user/check/{username}")
    suspend fun checkUsername(@Path("username") username: String): Response<UsernamevalidationResponse>

    @DELETE("user/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String): Response<DeleteUserResponse>
}