package com.sceyt.chatuikit.persistence

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T : Any?> lazyVar(initializer: () -> T): ReadWriteProperty<Any?, T> {
    return LazyVarDelegate(initializer)
}

class LazyVarDelegate<T : Any?>(initializer: () -> T) : ReadWriteProperty<Any?, T> {
    private val defaultValue: T by lazy(initializer)
    private var overrideValue: T? = null

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        overrideValue = value
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return overrideValue ?: defaultValue
    }
}