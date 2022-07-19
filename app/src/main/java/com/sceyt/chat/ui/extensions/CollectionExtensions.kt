package com.sceyt.chat.ui.extensions

inline fun <T> Collection<T>.findIndexed(predicate: (T) -> Boolean): Pair<Int, T>? {
    forEachIndexed { index: Int, item: T ->
        if (predicate(item)) {
            return Pair(index, item)
        }
    }
    return null
}

fun <T> List<T>.updateCommon(newList: List<T>, predicate: (old: T, new: T) -> Boolean): List<T> {
    val updateList = toMutableList()
    newList.forEach { newListElement ->
        findIndexed {
            predicate(it, newListElement)
        }?.let {
            updateList[it.first] = newListElement
        } ?: run { updateList.add(newListElement) }
    }
    return updateList
}