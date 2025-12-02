package com.sceyt.chatuikit.data.models

import com.sceyt.chat.models.SceytException

sealed class SceytResponse<T>(
    val data: T? = null,
    val message: String? = null,
    val code: Int? = null,
) {
    class Success<T>(
        data: T?,
    ) : SceytResponse<T>(data) {

        override fun toString(): String {
            return "Success(data=$data)"
        }
    }

    class Error<T>(
        val exception: SceytException? = null,
        data: T? = null,
    ) : SceytResponse<T>(data, exception?.message, exception?.code) {

        override fun toString(): String {
            return "Error(exception=$exception)"
        }
    }
}


sealed class SceytPagingResponse<T>(
    val message: String? = null,
    val code: Int? = null,
) {
    class Success<T>(
        val data: T,
        val hasNext: Boolean,
        val nextToken: String? = null,
    ) : SceytPagingResponse<T>() {

        override fun toString(): String {
            return "Success(data=$data, hasNext=$hasNext, nextToken=$nextToken)"
        }
    }

    class Error<T>(
        val exception: SceytException? = null,
    ) : SceytPagingResponse<T>(exception?.message, exception?.code) {

        override fun toString(): String {
            return "Error(exception=$exception)"
        }
    }
}

fun <T> createErrorResponse(message: String, code: Int = 0): SceytResponse.Error<T> {
    val exception = createSceytException(message, code)
    return SceytResponse.Error(exception)
}

fun createSceytException(message: String, code: Int = 0): SceytException {
    return SceytException(code, message)
}