package com.sceyt.chat.ui.data.models

sealed class SceytResponse<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T?) : SceytResponse<T>(data)
    class Error<T>(message: String? = null, data: T? = null) : SceytResponse<T>(data, message)
}
