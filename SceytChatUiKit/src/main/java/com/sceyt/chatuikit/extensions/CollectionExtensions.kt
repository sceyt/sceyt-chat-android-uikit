package com.sceyt.chatuikit.extensions

import androidx.core.util.Predicate

inline fun <T> Collection<T>.findIndexed(predicate: (T) -> Boolean): Pair<Int, T>? {
    forEachIndexed { index: Int, item: T ->
        if (predicate(item)) {
            return Pair(index, item)
        }
    }
    return null
}

inline fun <T> ArrayList<T>.findLastIndexed(predicate: (T) -> Boolean): Pair<Int, T>? {
    if (isEmpty()) return null
    for (i in indices.reversed()) {
        val item = get(i)
        if (predicate(item)) {
            return Pair(i, item)
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

fun <T> MutableIterable<T>.removeAllIf(predicate: Predicate<T>): Boolean {
    val itr = iterator()
    var removed = false
    while (itr.hasNext()) {
        val t = itr.next()
        if (predicate.test(t)) {
            itr.remove()
            removed = true
        }
    }
    return removed
}

fun <K, V> HashMap<K, V>.removeAllIf(predicate: Predicate<V>): Boolean {
    var removed = false
    val iterator = entries.iterator()
    while (iterator.hasNext()) {
        val t = iterator.next()
        if (predicate.test(t.value)) {
            iterator.remove()
            removed = true
        }
    }
    return removed
}