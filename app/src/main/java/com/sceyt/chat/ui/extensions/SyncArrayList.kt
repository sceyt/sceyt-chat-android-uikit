package com.sceyt.chat.ui.extensions

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.UnaryOperator
import java.util.stream.Stream
import kotlin.collections.ArrayList

class SyncArrayList<T> : ArrayList<T> {

    constructor()

    constructor(collection: Collection<T>) {
        addAll(collection)
    }

    private val syncObj = Any()

    override fun add(element: T): Boolean {
        synchronized(syncObj) {
            return super.add(element)
        }
    }

    override fun add(index: Int, element: T) {
        synchronized(syncObj) {
            super.add(index, element)
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        synchronized(syncObj) {
            return super.addAll(elements)
        }
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        synchronized(syncObj) {
            return super.addAll(index, elements)
        }
    }

    override fun get(index: Int): T {
        synchronized(syncObj) {
            return super.get(index)
        }
    }

    override fun set(index: Int, element: T): T {
        synchronized(syncObj) {
            return super.set(index, element)
        }
    }

    override fun contains(element: T): Boolean {
        synchronized(syncObj) {
            return super.contains(element)
        }
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        synchronized(syncObj) {
            return super.containsAll(elements)
        }
    }

    override fun remove(element: T): Boolean {
        synchronized(syncObj) {
            return super.remove(element)
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        synchronized(syncObj) {
            return super.removeAll(elements)
        }
    }

    override fun removeAt(index: Int): T {
        synchronized(syncObj) {
            return super.removeAt(index)
        }
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        synchronized(syncObj) {
            super.removeRange(fromIndex, toIndex)
        }
    }

    override fun clear() {
        synchronized(syncObj) {
            super.clear()
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        synchronized(syncObj) {
            return super.subList(fromIndex, toIndex)
        }
    }

    override fun equals(other: Any?): Boolean {
        synchronized(syncObj) {
            return super.equals(other)
        }
    }

    override fun clone(): Any {
        synchronized(syncObj) {
            return super.clone()
        }
    }

    override fun isEmpty(): Boolean {
        synchronized(syncObj) {
            return super.isEmpty()
        }
    }

    override fun indexOf(element: T): Int {
        synchronized(syncObj) {
            return super.indexOf(element)
        }
    }

    override fun listIterator(): MutableListIterator<T> {
        synchronized(syncObj) {
            return super.listIterator()
        }
    }

    override fun iterator(): MutableIterator<T> {
        synchronized(syncObj) {
            return super.iterator()
        }
    }

    override fun ensureCapacity(minCapacity: Int) {
        synchronized(syncObj) {
            super.ensureCapacity(minCapacity)
        }
    }

    override fun toArray(): Array<Any> {
        synchronized(syncObj) {
            return super.toArray()
        }
    }

    override fun toString(): String {
        synchronized(syncObj) {
            return super.toString()
        }
    }

    override fun hashCode(): Int {
        synchronized(syncObj) {
            return super.hashCode()
        }
    }

    override fun spliterator(): Spliterator<T> {
        synchronized(syncObj) {
            return super.spliterator()
        }
    }

    override fun trimToSize() {
        synchronized(syncObj) {
            super.trimToSize()
        }
    }

    override fun lastIndexOf(element: T): Int {
        synchronized(syncObj) {
            return super.lastIndexOf(element)
        }
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        synchronized(syncObj) {
            return super.listIterator(index)
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        synchronized(syncObj) {
            return super.retainAll(elements)
        }
    }

    override fun stream(): Stream<T> {
        synchronized(syncObj) {
            return super.stream()
        }
    }

    override fun <T : Any?> toArray(a: Array<out T>): Array<T> {
        synchronized(syncObj) {
            return super.toArray(a)
        }
    }

    override fun parallelStream(): Stream<T> {
        synchronized(syncObj) {
            return super.parallelStream()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun replaceAll(operator: UnaryOperator<T>) {
        synchronized(syncObj) {
            super.replaceAll(operator)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun removeIf(filter: Predicate<in T>): Boolean {
        synchronized(syncObj) {
            return super.removeIf(filter)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun forEach(action: Consumer<in T>) {
        synchronized(syncObj) {
            super.forEach(action)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun sort(c: Comparator<in T>?) {
        synchronized(syncObj) {
            super.sort(c)
        }
    }
}