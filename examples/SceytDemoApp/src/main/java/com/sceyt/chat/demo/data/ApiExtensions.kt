package com.sceyt.chat.demo.data

import retrofit2.HttpException
import retrofit2.Response
import java.net.HttpURLConnection.HTTP_NO_CONTENT

suspend fun <T> makeApiCall(call: suspend () -> Response<T>): Result<T?> {
    try {
        val response = call()
        return when {
            response.isSuccessful -> {
                val body = response.body()
                when {
                    body != null -> Result.success(body)
                    response.code() == HTTP_NO_CONTENT -> Result.success(null)
                    else -> error(Exception("Response body is null"))
                }
            }

            else -> {
                error(HttpException(response))
            }
        }
    } catch (e: Exception) {
        return error(e)
    }
}


fun <T> error(response: Throwable): Result<T> {
    return Result.failure(response)
}




