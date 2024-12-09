package com.sceyt.chat.demo.data.repositories

import com.sceyt.chat.demo.data.api.UserApiService
import com.sceyt.chat.demo.data.models.DeleteUserResponse
import com.sceyt.chat.demo.data.models.UsernamevalidationResponse
import retrofit2.HttpException

class UserRepository(
    private val userApiService: UserApiService
) {

    suspend fun checkUsername(username: String): Result<UsernamevalidationResponse> {
        return try {
            val response = userApiService.checkUsername(username)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(HttpStatusException(response.code(), response.message()))
            }
        } catch (e: HttpException) {
            Result.failure(HttpStatusException(e.code(), e.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<DeleteUserResponse> {
        return try {
            val response = userApiService.deleteUser(userId)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(HttpStatusException(response.code(), response.message()))
            }
        } catch (e: HttpException) {
            Result.failure(HttpStatusException(e.code(), e.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class HttpStatusException(val statusCode: Int, message: String) : Exception(message)
