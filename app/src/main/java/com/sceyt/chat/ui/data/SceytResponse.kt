package com.sceyt.chat.ui.data

sealed class SceytResponse<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T?) : SceytResponse<T>(data)
    class Error<T>(message: String?, data: T? = null) : SceytResponse<T>(data, message)
    class Loading<T>(val isLoading: Boolean = true) : SceytResponse<T>(null)
}
