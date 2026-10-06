package com.sceyt.chat.connection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HttpChatTokenProviderTest {

    @Test
    fun fetchesTokenForUser() = runTest {
        lateinit var capturedRequest: Request
        val client = clientReturning("{\"token\":\"token-1\"}") { request ->
            capturedRequest = request
        }
        val provider = HttpChatTokenProvider(
            endpoint = "https://example.com/token?source=android",
            userIdQueryParameter = "member",
            headers = mapOf("Authorization" to "Bearer api-token"),
            client = client
        )

        val token = provider.provideToken("alice smith")

        assertThat(token).isEqualTo("token-1")
        assertThat(capturedRequest.url.queryParameter("member")).isEqualTo("alice smith")
        assertThat(capturedRequest.url.queryParameter("source")).isEqualTo("android")
        assertThat(capturedRequest.header("Authorization")).isEqualTo("Bearer api-token")
    }

    @Test
    fun httpFailureIsExposed() = runTest {
        val provider = HttpChatTokenProvider(
            endpoint = "https://example.com/token",
            userIdQueryParameter = "user",
            headers = emptyMap(),
            client = clientReturning(body = "", code = 401)
        )

        val error = runCatching { provider.provideToken("alice") }.exceptionOrNull()

        assertThat(error).isInstanceOf(IOException::class.java)
        assertThat(error).hasMessageThat().isEqualTo("Token request failed with HTTP 401")
    }

    @Test
    fun invalidResponseIsExposed() = runTest {
        val provider = HttpChatTokenProvider(
            endpoint = "https://example.com/token",
            userIdQueryParameter = "user",
            headers = emptyMap(),
            client = clientReturning("{\"value\":\"missing-token\"}")
        )

        val error = runCatching { provider.provideToken("alice") }.exceptionOrNull()

        assertThat(error).isInstanceOf(IOException::class.java)
        assertThat(error).hasMessageThat().isEqualTo("Token response does not contain a token")
    }

    @Test
    fun nonStringTokenIsRejected() = runTest {
        val provider = HttpChatTokenProvider(
            endpoint = "https://example.com/token",
            userIdQueryParameter = "user",
            headers = emptyMap(),
            client = clientReturning("{\"token\":123}")
        )

        val error = runCatching { provider.provideToken("alice") }.exceptionOrNull()

        assertThat(error).isInstanceOf(IOException::class.java)
        assertThat(error).hasMessageThat().isEqualTo("Token response is invalid")
    }

    @Test
    fun cancellationCancelsHttpCall() = runTest {
        val requestStarted = CountDownLatch(1)
        val requestCancelled = CountDownLatch(1)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestStarted.countDown()
                val timeoutAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
                while (!chain.call().isCanceled() && System.nanoTime() < timeoutAt) {
                    Thread.sleep(10L)
                }
                if (chain.call().isCanceled()) {
                    requestCancelled.countDown()
                }
                throw IOException("Canceled")
            }
            .build()
        val provider = HttpChatTokenProvider(
            endpoint = "https://example.com/token",
            userIdQueryParameter = "user",
            headers = emptyMap(),
            client = client
        )

        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            provider.provideToken("alice")
        }
        assertThat(requestStarted.await(1, TimeUnit.SECONDS)).isTrue()

        request.cancelAndJoin()

        assertThat(requestCancelled.await(1, TimeUnit.SECONDS)).isTrue()
    }

    private fun clientReturning(
        body: String,
        code: Int = 200,
        onRequest: (Request) -> Unit = {}
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            onRequest(request)
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("Test response")
                .body(body.toResponseBody())
                .build()
        }
        .build()
}
