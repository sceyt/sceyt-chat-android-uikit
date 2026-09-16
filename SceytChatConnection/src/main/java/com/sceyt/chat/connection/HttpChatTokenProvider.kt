package com.sceyt.chat.connection

import com.google.gson.JsonParser
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Fetches chat tokens with a GET request containing the user ID as a query parameter.
 * The endpoint response must be a JSON object containing a non-empty `token` string.
 */
class HttpChatTokenProvider internal constructor(
    endpoint: String,
    private val userIdQueryParameter: String,
    headers: Map<String, String>,
    private val client: OkHttpClient
) : ChatTokenProvider {

    constructor(
        endpoint: String,
        userIdQueryParameter: String = DEFAULT_USER_ID_QUERY_PARAMETER,
        headers: Map<String, String> = emptyMap()
    ) : this(endpoint, userIdQueryParameter, headers, HTTP_CLIENT)

    private val endpoint = endpoint.toHttpUrl()
    private val headers = headers.toMap()

    init {
        require(userIdQueryParameter.isNotBlank()) {
            "userIdQueryParameter must not be blank"
        }
    }

    override suspend fun provideToken(userId: String): String {
        val url = endpoint.newBuilder()
            .setQueryParameter(userIdQueryParameter, userId)
            .build()
        val request = Request.Builder()
            .url(url)
            .apply {
                headers.forEach { (name, value) -> addHeader(name, value) }
            }
            .get()
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching { response.use(::readToken) }
                    if (continuation.isActive) {
                        result.fold(
                            onSuccess = continuation::resume,
                            onFailure = continuation::resumeWithException
                        )
                    }
                }
            })
        }
    }

    private fun readToken(response: Response): String {
        if (!response.isSuccessful) {
            throw IOException("Token request failed with HTTP ${response.code}")
        }

        val tokenElement = try {
            JsonParser.parseString(response.body.string())
                .asJsonObject
                .get(TOKEN_FIELD)
        } catch (error: Exception) {
            throw IOException("Token response is invalid", error)
        }

        if (tokenElement != null &&
            (!tokenElement.isJsonPrimitive || !tokenElement.asJsonPrimitive.isString)
        ) {
            throw IOException("Token response is invalid")
        }

        return tokenElement?.asString?.takeIf { it.isNotBlank() }
            ?: throw IOException("Token response does not contain a token")
    }

    private companion object {
        const val DEFAULT_USER_ID_QUERY_PARAMETER = "user"
        const val TOKEN_FIELD = "token"
        val HTTP_CLIENT = OkHttpClient()
    }
}
