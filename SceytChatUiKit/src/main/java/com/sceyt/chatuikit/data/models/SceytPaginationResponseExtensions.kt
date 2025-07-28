package com.sceyt.chatuikit.data.models

import com.sceyt.chat.models.SceytException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
@SinceKotlin("1.3")
inline fun <T> SceytPagingResponse<T>.onSuccess(
        action: (value: SceytPagingResponse.Success<T>) -> Unit,
): SceytPagingResponse<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (this is SceytPagingResponse.Success)
        action(this)

    return this
}

@OptIn(ExperimentalContracts::class)
@SinceKotlin("1.3")
inline fun <T> SceytPagingResponse<T>.onError(
        action: (value: SceytException?) -> Unit,
): SceytPagingResponse<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (this is SceytPagingResponse.Error)
        action(exception)

    return this
}

@OptIn(ExperimentalContracts::class)
@SinceKotlin("1.3")
inline fun <R, T> SceytPagingResponse<T>.fold(
        onSuccess: (value: SceytPagingResponse.Success<T>) -> R,
        onError: (exception: SceytException?) -> R,
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is SceytPagingResponse.Success -> onSuccess(this)
        is SceytPagingResponse.Error -> onError(exception)
    }
}
