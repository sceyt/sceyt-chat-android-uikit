package com.sceyt.chatuikit.presentation.uicomponents.searchinput

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.extensions.maybeComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    constructor(debounceMs: Long, fragment: Fragment) {
        this.debounceMs = debounceMs
        this.scope = fragment.lifecycleScope
    }

    constructor(debounceMs: Long, view: View) {
        this.debounceMs = debounceMs
        this.scope = (view.findViewTreeLifecycleOwner()
                ?: view.context.maybeComponentActivity())?.lifecycleScope
                ?: CoroutineScope(Dispatchers.Main + SupervisorJob())
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
            if (isActive)
                work()
        }
    }

    /**
     * Cancels the previous work and launches a new coroutine
     * Run immediately if no job is running.
     * containing the new work.
     */
    fun submitForceIfNotRunning(work: () -> Unit) {
        val needToDelay = job?.isActive ?: false
        job?.cancel()
        job = scope.launch {
            if (needToDelay)
                delay(debounceMs)
            if (isActive)
                work()
            //Keep job alive for next submit
            if (!needToDelay)
                delay(debounceMs)
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
            if (isActive)
                work()
        }
    }

    /**
     * Cancels the current work without shutting down the Coroutine scope.
     * Return is last job have been active.
     */
    fun cancelLastDebounce(): Boolean {
        val isActive = job?.isActive
        job?.cancel()
        return isActive ?: false
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
