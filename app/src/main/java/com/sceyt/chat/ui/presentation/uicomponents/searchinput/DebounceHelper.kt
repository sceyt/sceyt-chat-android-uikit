package com.sceyt.chat.ui.presentation.uicomponents.searchinput

import kotlinx.coroutines.*

/**
 * Utility class for debouncing high frequency events.
 *
 * [submit]ting a new piece of work to run within the debounce window
 * will cancel the previously submitted pending work.
 */
class DebounceHelper {

    constructor(debounceMs: Long) {
        this.debounceMs = debounceMs
        this.scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    constructor(debounceMs: Long, scope: CoroutineScope) {
        this.debounceMs = debounceMs
        this.scope = scope
    }

    private val debounceMs: Long
    private val scope: CoroutineScope
    private var job: Job? = null

    /**
     * Cancels the previous work and launches a new coroutine
     * containing the new work.
     */
    fun submit(work: () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(debounceMs)
            work()
        }
    }

    /**
     * Cancels the previous work and launches a new coroutine
     * containing the new suspendable work.
     */
    fun submitSuspendable(work: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(debounceMs)
            work()
        }
    }

    /**
     * Cancels the current work without shutting down the Coroutine scope.
     */
    fun cancelLastDebounce() {
        job?.cancel()
    }

    /**
     * Cleans up any pending work.
     *
     * Note that a shut down DebounceHelper will never execute work again.
     * Note be careful when the scope is set with constructor.
     */
    fun shutdown() {
        scope.cancel()
    }
}
