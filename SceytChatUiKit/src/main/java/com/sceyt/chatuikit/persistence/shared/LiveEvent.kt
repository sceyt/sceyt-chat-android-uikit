package com.sceyt.chatuikit.persistence.shared

import androidx.annotation.MainThread
import androidx.collection.ArraySet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

class LiveEvent<T> : MediatorLiveData<T>() {

    private val observers = ArraySet<ObserverWrapper<in T>>()

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val wrapper = ObserverWrapper(observer)
        observers.add(wrapper)
        super.observe(owner, wrapper)
    }

    @MainThread
    override fun observeForever(observer: Observer<in T>) {
        val wrapper = ObserverWrapper(observer)
        observers.add(wrapper)
        super.observeForever(wrapper)
    }

    @MainThread
    override fun removeObserver(observer: Observer<in T>) {
        val iterator = observers.iterator()

        while (iterator.hasNext()) {
            val wrapper = iterator.next()

            if (wrapper === observer || wrapper.observer === observer) {
                iterator.remove()
                super.removeObserver(wrapper)
                return
            }
        }

        super.removeObserver(observer)
    }

    @MainThread
    override fun setValue(value: T?) {
        observers.toList().forEach { it.newValue() }
        super.setValue(value)
    }

    private class ObserverWrapper<T>(
        val observer: Observer<T>,
    ) : Observer<T> {

        private var pending = false

        override fun onChanged(value: T) {
            if (!pending) return

            pending = false
            observer.onChanged(value)
        }

        fun newValue() {
            pending = true
        }
    }
}
