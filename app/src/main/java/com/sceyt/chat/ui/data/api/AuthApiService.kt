package com.sceyt.chat.ui.data.api

import com.sceyt.chat.ui.data.models.GetTokenData
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @GET("dev/user/genToken")
    suspend fun getSceytToken(@Query("user") userId: String): Response<GetTokenData>
}