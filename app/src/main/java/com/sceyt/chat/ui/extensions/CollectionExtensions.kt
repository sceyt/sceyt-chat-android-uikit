package com.sceyt.chat.ui.extensions

inline fun <reified T> List<T>.findIndexed(predicate: (T) -> Boolean): Pair<Int, T>? {
    forEachIndexed { index: Int, item: T ->
        if (predicate(item)) {
            return Pair(index, item)
        }
    }
    return null
}
