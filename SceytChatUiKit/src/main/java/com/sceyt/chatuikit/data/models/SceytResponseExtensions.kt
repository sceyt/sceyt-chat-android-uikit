package com.sceyt.chatuikit.data.models

import com.sceyt.chat.models.SceytException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


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
inline fun <T> SceytResponse<T>.onSuccessNotNull(action: (value: T) -> Unit): SceytResponse<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (this is SceytResponse.Success && data != null)
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
inline fun <R, T> SceytResponse<T>.fold(
        onSuccess: (value: T?) -> R,
        onError: (exception: SceytException?) -> R,
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is SceytResponse.Success -> onSuccess(data)
        is SceytResponse.Error -> onError(exception)
    }
}


