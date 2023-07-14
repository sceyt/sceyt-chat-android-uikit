package com.sceyt.chat.ui.data.interceptors

import com.sceyt.sceytchatuikit.logger.SceytLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class RetryInterceptor(private val maxRetries: Int,
                       private val retryIntervalMillis: Long) : Interceptor {

    private companion object {
        private const val Tag = "RetryInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var response: Response? = null
        var retryCount = 0
        var delayMillis = retryIntervalMillis

        suspend fun doRetry(throwable: Throwable) {
            retryCount++

            if (retryCount >= maxRetries) {
                SceytLog.i(Tag, "intercept: error :  reach the maximum retry count ${throwable.message} $retryCount")
                throw throwable // Throw exception if we reach the maximum retry count
            }
            SceytLog.i(Tag, "intercept: error : ${throwable.message} $retryCount")
            delay(delayMillis)
            delayMillis *= 2
        }

        runBlocking {
            while (response == null && retryCount < maxRetries) {
                try {
                    SceytLog.i(Tag, " intercept: start process retryCount: $retryCount delay: $delayMillis")
                    response = chain.proceed(chain.request())
                    val responseCode = response?.code
                    val responseIsSuccessful = response?.isSuccessful
                    if (responseIsSuccessful != true) {
                        response?.close()
                        response = null
                        doRetry(IllegalStateException("Response is not successful, code == $responseCode"))
                    }
                } catch (e: Throwable) {
                    doRetry(e)
                }
            }
        }

        return response ?: throw IllegalStateException("Failed to execute the request")
    }
}
