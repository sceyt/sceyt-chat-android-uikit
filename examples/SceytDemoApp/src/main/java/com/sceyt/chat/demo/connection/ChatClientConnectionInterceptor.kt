package com.sceyt.chat.demo.connection

import com.sceyt.chat.demo.connection.SceytConnectionProvider.Companion.Tag
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.repositories.ConnectionRepo
import com.sceyt.chatuikit.logger.SceytLog
import java.util.concurrent.atomic.AtomicBoolean

class ChatClientConnectionInterceptor(private val connectionRepo: ConnectionRepo,
                                      private val preference: AppSharedPreference) {

    private var processing = AtomicBoolean(false)

    suspend fun getChatToken(userId: String): String? {
        SceytLog.i(Tag, "$Tag getChatToken invoked. processing: ${processing.get()}")
        if (processing.get()) return null
        processing.set(true)
        var sceytToken: String? = null
        val response = getSceytToken(userId)
        if (response.isSuccess)
            sceytToken = response.getOrNull()
        processing.set(false)
        return sceytToken
    }

    private suspend fun getSceytToken(userId: String): Result<String> {
        SceytLog.i(Tag, "$Tag try to get Sceyt token")
        val result = connectionRepo.getSceytToken(userId)

        return if (result.isSuccess) {
            val token: String? = result.getOrNull()?.token
            SceytLog.i(Tag, "Sceyt token success, token ${token?.take(8)}")
            preference.setString(AppSharedPreference.PREF_USER_TOKEN, token)
            preference.setString(AppSharedPreference.PREF_USER_ID, userId)
            token?.let {
                Result.success(it)
            } ?: Result.failure(Throwable("Sceyt token is null"))
        } else {
            val message = "couldn't get SceytToken: " + result.exceptionOrNull()?.message
            SceytLog.e(Tag, message)
            Result.failure(Throwable(message))
        }
    }
}