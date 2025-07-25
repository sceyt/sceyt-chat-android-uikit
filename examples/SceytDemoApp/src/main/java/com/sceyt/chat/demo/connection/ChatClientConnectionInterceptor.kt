package com.sceyt.chat.demo.connection

import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.repositories.ConnectionRepo
import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChatClientConnectionInterceptor(
        private val connectionRepo: ConnectionRepo,
        private val preference: AppSharedPreference,
) {

    private val mutex = Mutex()
    private var ongoingRequest: Deferred<Result<String>>? = null

    suspend fun getChatToken(userId: String): String? {
        val wasLocked = mutex.isLocked
        return mutex.withLock {
            if (!wasLocked && ongoingRequest != null) {
                // Previous request finished and we're starting fresh batch
                SceytLog.i(TAG, "$TAG getChatToken clearing old cached result for freshness")
                ongoingRequest = null
            }

            ongoingRequest?.await()
                ?.onSuccess {
                    SceytLog.i(TAG, "$TAG getChatToken got result from shared request: ${it.take(8)}")
                    return@withLock it
                }?.onFailure {
                    // Allow to make request others on failure
                    SceytLog.i(TAG, "$TAG getChatToken shared request failed, clearing for retry")
                    ongoingRequest = null
                }

            SceytLog.i(TAG, "$TAG getChatToken starting new request")
            coroutineScope {
                ongoingRequest = async { getSceytTokenImpl(userId) }

                return@coroutineScope ongoingRequest?.await()?.fold(
                    onSuccess = {
                        SceytLog.i(TAG, "✅ Sceyt token success, token ${it.takeLast(8)}")
                        it
                    },
                    onFailure = {
                        SceytLog.e(TAG, "couldn't get SceytToken: ${it.message}")
                        null
                    })
            }
        }
    }

    private suspend fun getSceytTokenImpl(userId: String): Result<String> {
        SceytLog.i(TAG, "$TAG try to get Sceyt token")
        val result = connectionRepo.getSceytToken(userId)

        return if (result.isSuccess) {
            val token: String? = result.getOrNull()?.token
            SceytLog.i(TAG, "Sceyt token success, token ${token?.takeLast(8)}")
            preference.setString(AppSharedPreference.PREF_USER_TOKEN, token)
            preference.setString(AppSharedPreference.PREF_USER_ID, userId)
            token?.ifBlank { null }?.let {
                Result.success(it)
            } ?: Result.failure(Throwable("Sceyt token is null"))
        } else {
            val message = "couldn't get SceytToken: " + result.exceptionOrNull()?.message
            SceytLog.e(TAG, message)
            Result.failure(Throwable(message))
        }
    }

    companion object {
        private const val TAG = "ChatClientConnectionInterceptor"
    }
}