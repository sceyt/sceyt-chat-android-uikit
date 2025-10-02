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
    ) : SceytPagingResponse<T>() {

        override fun toString(): String {
            return "Success(data=$data, hasNext=$hasNext)"
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
