package com.sceyt.chatuikit.data.models

import com.sceyt.chat.models.SceytException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class SceytResponse<T>(val data: T? = null, val message: String? = null, val code: Int? = null) {
    class Success<T>(data: T?) : SceytResponse<T>(data)
    class Error<T>(val exception: SceytException? = null, data: T? = null) : SceytResponse<T>(data, exception?.message, exception?.code)
}

sealed class SceytPagingResponse<T>(val data: T? = null, val message: String? = null, val code: Int? = null) {
    class Success<T>(data: T?, val hasNext: Boolean) : SceytPagingResponse<T>(data)
    class Error<T>(val exception: SceytException? = null, data: T? = null) : SceytPagingResponse<T>(data, exception?.message, exception?.code)
}

@OptIn(ExperimentalContracts::class)
@SinceKotlin("1.3")
inline fun <T> SceytResponse<T>.onSuccess(action: (value: T?) -> Unit): SceytResponse<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (this is SceytResponse.Success)
        action(data)

    return this
}

@OptIn(ExperimentalContracts::class)
@SinceKotlin("1.3")
inline fun <T> SceytResponse<T>.onError(action: (value: SceytException?) -> Unit): SceytResponse<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (this is SceytResponse.Error)
        action(exception)

    return this
}

@OptIn(ExperimentalContracts::class)
@SinceKotlin("1.3")
inline fun <T> SceytResponse<T>.fold(
        onSuccess: (value: T?) -> Unit,
        onError: (value: SceytException?) -> Unit
): SceytResponse<T> {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    }
    when (this) {
        is SceytResponse.Success -> onSuccess(data)
        is SceytResponse.Error -> onError(exception)
    }
    return this
}