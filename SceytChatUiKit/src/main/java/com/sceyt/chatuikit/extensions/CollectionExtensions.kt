@file:Suppress("unused")

package com.sceyt.chatuikit.extensions

inline fun <T> Collection<T>.findIndexed(predicate: (T) -> Boolean): Pair<Int, T>? {
    for ((index, item) in withIndex()) {
        if (predicate(item)) return index to item
    }
    return null
}

inline fun <T> List<T>.findLastIndexed(predicate: (T) -> Boolean): Pair<Int, T>? {
    for (i in indices.reversed()) {
        val item = get(i)
        if (predicate(item)) return i to item
    }
    return null
}

fun <T> List<T>.updateCommon(
    newList: List<T>,
    predicate: (old: T, new: T) -> Boolean
): List<T> {
    val updatedList = toMutableList()

    newList.forEach { newItem ->
        // Try to find index of matching old item
        val index = updatedList.indexOfFirst { predicate(it, newItem) }

        if (index >= 0) {
            // Replace existing item
            updatedList[index] = newItem
        } else {
            // Add new item
            updatedList.add(newItem)
        }
    }

    return updatedList
}

inline fun <T> MutableIterable<T>.removeAllIf(predicate: (T) -> Boolean): Boolean {
    var removed = false
    val itr = iterator()
    while (itr.hasNext()) {
        val element = itr.next()
        if (predicate(element)) {
            itr.remove()
            removed = true
        }
    }
    return removed
}

inline fun <K, V> MutableMap<K, V>.removeAllIf(predicate: (V) -> Boolean): Boolean {
    var removed = false
    val iterator = entries.iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        if (predicate(entry.value)) {
            iterator.remove()
            removed = true
        }
    }
    return removed
}

inline fun <K, V> MutableMap<K, V>.removeAllIfCollect(
    predicate: (V) -> Boolean
): List<V> {
    val removed = mutableListOf<V>()
    val iterator = entries.iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        if (predicate(entry.value)) {
            removed.add(entry.value)
            iterator.remove()
        }
    }
    return removed
}

inline fun <K, V> HashMap<K, V>.forEachKeyValue(action: (K, V) -> Unit) {
    forEach { entries ->
        action(entries.key, entries.value)
    }
}
