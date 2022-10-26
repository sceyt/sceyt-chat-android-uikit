package com.sceyt.sceytchatuikit.data.models

import com.sceyt.chat.models.SceytException

sealed class SceytResponse<T>(val data: T? = null, val message: String? = null, val code: Int? = null) {
    class Success<T>(data: T?) : SceytResponse<T>(data)
    class Error<T>(val exception: SceytException? = null, data: T? = null) : SceytResponse<T>(data, exception?.message, exception?.code)
}
