package com.sceyt.chatuikit.persistence.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    return ArrayList(this)
}

inline fun <T> Continuation<T>.safeResume(value: T, onExceptionCalled: () -> Unit = {}) {
    try {
        resume(value)
    } catch (ex: Exception) {
        onExceptionCalled()
    }
}

fun <T> MutableLiveData<T>.asLiveData(): LiveData<T> {
    return this
}

fun <T> MutableCollection<T>.removeFirstIf(filter: (T) -> Boolean): Int {
    val each = iterator()
    var index = 0
    while (each.hasNext()) {
        if (filter(each.next())) {
            each.remove()
            return index
        }
        index++
    }
    return -1
}