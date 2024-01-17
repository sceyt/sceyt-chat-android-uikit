package com.sceyt.chat.demo.data.interceptors

import com.sceyt.sceytchatuikit.logger.SceytLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

class RetryInterceptor(private val maxRetries: Int,
                       private val retryIntervalMillis: Long) : Interceptor {

    private companion object {
        private const val Tag = "RetryInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var response: Response? = null
        var retryCount = 0
        var delayMillis = retryIntervalMillis
        var errorCode = 500
        var errorMessage = "RetryInterceptor: response is null"

        suspend fun doRetry(throwable: Throwable) {
            retryCount++

            if (retryCount >= maxRetries) {
                SceytLog.i(Tag, "intercept: error :  reach the maximum retry count retryCount:$retryCount, error:${throwable.message} ")
                return
            }
            SceytLog.i(Tag, "intercept: error : ${throwable.message} $retryCount")
            delay(delayMillis)
            delayMillis *= 2
        }

        runBlocking {
            while (retryCount < maxRetries) {
                try {
                    SceytLog.i(Tag, "intercept: start process retryCount: $retryCount delay: $delayMillis")
                    response = chain.proceed(chain.request())
                    val responseCode = response?.code
                    val responseIsSuccessful = response?.isSuccessful

                    if (responseIsSuccessful == true) {
                        break
                    } else {
                        response?.close()
                        doRetry(IllegalStateException("Response is not successful, code == $responseCode"))
                    }
                } catch (e: Throwable) {
                    when (e) {
                        is HttpException -> errorCode = e.code()
                        is SocketTimeoutException -> errorCode = 408 // Request Timeout
                        is IOException -> errorCode = 502 // Bad Gateway
                    }
                    errorMessage = e.message ?: errorMessage
                    doRetry(e)
                }
            }
        }

        return response ?: Response.Builder() // return response with code 500 if response is null
            .code(errorCode)
            .request(chain.request())
            .protocol(Protocol.HTTP_2)
            .message(errorMessage)
            .build()
    }
}