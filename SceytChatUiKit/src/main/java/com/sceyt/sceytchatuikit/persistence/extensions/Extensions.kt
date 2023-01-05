package com.sceyt.sceytchatuikit.persistence.extensions

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

inline fun <reified T : Enum<T>> Int.toEnum(): T = enumValues<T>()[this]

fun String?.equalsIgnoreNull(other: String?): Boolean {
    return (this?.trim() ?: "") == (other?.trim() ?: "")
}

fun <T> List<T>?.equalsIgnoreNull(other: List<T>?): Boolean {
    return (this ?: listOf()) == (other ?: listOf<T>())
}

inline fun <reified T> Array<T>?.equalsIgnoreNull(other: Array<T>?): Boolean {
    return (this ?: arrayOf()).contentEquals((other ?: arrayOf()))
}

fun <T> List<T>.toArrayList(): ArrayList<T> {
    return try {
        this as ArrayList
    } catch (ex: Exception) {
        ArrayList(this)
    }
}

inline fun <T> Continuation<T>.safeResume(value: T, onExceptionCalled: () -> Unit = {}) {
    try {
        resume(value)
    } catch (ex: Exception) {
        onExceptionCalled()
    }
}