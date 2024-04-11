package com.sceyt.chatuikit.presentation.common

import java.util.concurrent.ConcurrentHashMap

class ConcurrentHashSet<E : Any> : MutableSet<E> {
    private val map = ConcurrentHashMap<E, Any>()
    private val present = Any()

    override val size: Int
        get() = map.size

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun contains(element: E): Boolean = map.containsKey(element)

    override fun iterator(): MutableIterator<E> = map.keys.iterator()

    override fun add(element: E): Boolean = map.put(element, present) == null

    override fun remove(element: E): Boolean = map.remove(element) == present

    override fun containsAll(elements: Collection<E>): Boolean = elements.all { contains(it) }

    override fun addAll(elements: Collection<E>): Boolean = elements.fold(false) { acc, e -> acc or add(e) }

    override fun removeAll(elements: Collection<E>): Boolean = elements.fold(false) { acc, e -> acc or remove(e) }

    override fun retainAll(elements: Collection<E>): Boolean {
        val toRetain = elements.toSet()
        var modified = false
        val iterator = map.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (!toRetain.contains(key)) {
                iterator.remove()
                modified = true
            }
        }
        return modified
    }

    override fun clear() = map.clear()
}