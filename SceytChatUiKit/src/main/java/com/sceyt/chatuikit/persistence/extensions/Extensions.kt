package com.sceyt.chatuikit.persistence.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
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

inline fun <T> CancellableContinuation<T>.safeResume(value: T, onFailure: () -> Unit = {}) {
    try {
        if (isActive)
            resume(value)
    } catch (ex: Exception) {
        onFailure()
    }
}

fun <T> MutableLiveData<T>.asLiveData(): LiveData<T> {
    return this
}

fun <T> broadcastSharedFlow(
    replay: Int = 0,
    extraBufferCapacity: Int = 1,
    onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST
) = MutableSharedFlow<T>(
    replay = replay,
    extraBufferCapacity = extraBufferCapacity,
    onBufferOverflow = onBufferOverflow,
)

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