package com.sceyt.chatuikit.presentation.common.recyclerview

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.removeFirstIf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

class AsyncListDiffer<T : Any>(
    private val updateCallback: ListUpdateCallback,
    private val diffCallback: DiffUtil.ItemCallback<T>,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + backgroundDispatcher)
) {

    private val mutex = Mutex()

    constructor(
        adapter: RecyclerView.Adapter<*>,
        diffCallback: DiffUtil.ItemCallback<T>,
        scope: CoroutineScope
    ) : this(AdapterListUpdateCallback(adapter), diffCallback, scope = scope)


    private var list: List<T>? = null
    private var readOnlyList: List<T> = emptyList()
    private val listeners = mutableListOf<ListListener<T>>()
    private var lastSubmitJob: Job? = null

    @Volatile
    private var lastOperationsJob: Job? = null

    /**
     * Listener interface for when the current list is updated.
     */
    interface ListListener<T> {
        fun onCurrentListChanged(previousList: List<T>, currentList: List<T>)
    }

    /**
     * Get the current list. Returns an empty list if no list has been submitted.
     */
    val currentList: List<T>
        @MainThread get() = readOnlyList

    /**
     * Submit a new list for the adapter. Diff calculations will be computed on a background thread.
     * @param newList The new list to be submitted.
     */
    @MainThread
    fun submitList(newList: List<T>?, commitCallback: (() -> Unit)? = null) {
        submitListImpl(newList, commitCallback = commitCallback)
    }

    @MainThread
    fun updateItem(
            predicate: (T) -> Boolean,
            newItem: T,
            payloads: Any? = null,
            commitCallback: (() -> Unit)? = null,
    ) = scope.launch {
        mutex.withLock {
            waitForSubmitJob()
            val previousList = readOnlyList
            val newList = list?.toMutableList() ?: return@withLock
            val position = newList.indexOfFirst(predicate).takeIf { it != -1 } ?: return@withLock
            newList[position] = newItem
            performWithRetry {
                list = newList
                readOnlyList = Collections.unmodifiableList(newList)
                withContext(mainDispatcher) {
                    updateCallback.onChanged(position, 1, payloads)
                    onCurrentListChanged(previousList, commitCallback)
                }
            }
        }
    }.also { lastOperationsJob = it }

    @MainThread
    fun removeItem(
            commitCallback: (() -> Unit)? = null,
            predicate: (T) -> Boolean,
    ) = scope.launch {
        mutex.withLock {
            waitForSubmitJob()
            val previousList = readOnlyList
            val newList = list?.toMutableList() ?: mutableListOf()
            val position = newList.removeFirstIf(predicate).takeIf { it != -1 } ?: return@withLock
            performWithRetry {
                list = newList
                readOnlyList = Collections.unmodifiableList(newList)
                withContext(mainDispatcher) {
                    updateCallback.onRemoved(position, 1)
                    onCurrentListChanged(previousList, commitCallback)
                }
            }
            return@withLock
        }
    }.also { lastOperationsJob = it }

    @MainThread
    fun addItems(
            items: List<T>,
            commitCallback: (() -> Unit)? = null,
    ) = addItemsImpl(items, commitCallback = commitCallback)

    @MainThread
    fun addItems(
            position: Int,
            items: List<T>,
            commitCallback: (() -> Unit)? = null,
    ) = addItemsImpl(items, position = position, commitCallback = commitCallback)

    @MainThread
    fun addItem(
            item: T,
            commitCallback: (() -> Unit)? = null,
    ) = addItemsImpl(listOf(item), commitCallback = commitCallback)

    @MainThread
    fun addItem(
            position: Int,
            item: T,
            commitCallback: (() -> Unit)? = null,
    ) = addItemsImpl(listOf(item), position = position, commitCallback = commitCallback)

    /**
     * Add a listener to be notified when the current list changes.
     */
    @MainThread
    fun addListListener(listener: ListListener<T>) {
        listeners.add(listener)
    }

    /**
     * Remove a previously registered listener.
     */
    @MainThread
    fun removeListListener(listener: ListListener<T>) {
        listeners.remove(listener)
    }

    @WorkerThread
    private suspend fun calculateDiff(oldList: List<T>, newList: List<T>): DiffUtil.DiffResult {
        return withContext(backgroundDispatcher) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldList.size

                override fun getNewListSize(): Int = newList.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldList[oldItemPosition]
                    val newItem = newList[newItemPosition]
                    return diffCallback.areItemsTheSame(oldItem, newItem)
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem = oldList[oldItemPosition]
                    val newItem = newList[newItemPosition]
                    return diffCallback.areContentsTheSame(oldItem, newItem)
                }

                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                    return diffCallback.getChangePayload(
                        oldList[oldItemPosition],
                        newList[newItemPosition]
                    )
                }
            })
        }
    }

    private suspend fun latchList(
        newList: List<T>,
        diffResult: DiffUtil.DiffResult,
        commitCallback: (() -> Unit)?,
    ) = withContext(mainDispatcher) {
        if (!isActive) return@withContext
        performWithRetry {
            val previousList = readOnlyList
            list = newList
            readOnlyList = Collections.unmodifiableList(newList)
            diffResult.dispatchUpdatesTo(updateCallback)
            onCurrentListChanged(previousList, commitCallback)
        }
    }

    @MainThread
    private fun onCurrentListChanged(previousList: List<T>, commitCallback: (() -> Unit)?) {
        listeners.forEach { it.onCurrentListChanged(previousList, readOnlyList) }
        commitCallback?.invoke()
    }

    private fun submitListImpl(
            newList: List<T>?,
            commitCallback: (() -> Unit)? = null,
    ) {
        lastSubmitJob?.cancel()
        lastOperationsJob?.cancel()
        lastSubmitJob = scope.launch {
            val previousList = readOnlyList
            if (newList === list) {
                // Same list, nothing to do
                commitCallback?.invoke()
                return@launch
            }

            if (newList == null) {
                performWithRetry {
                    // Fast remove-all scenario
                    val countRemoved = list?.size ?: 0
                    list = null
                    readOnlyList = emptyList()
                    if (countRemoved == 0)
                        return@launch
                    updateCallback.onRemoved(0, countRemoved)
                    onCurrentListChanged(previousList, commitCallback)
                }
                return@launch
            }

            if (list == null) {
                if (newList.isEmpty()) return@launch
                performWithRetry {
                    // Fast insert-all scenario
                    list = newList
                    readOnlyList = Collections.unmodifiableList(newList)
                    updateCallback.onInserted(0, newList.size)
                    onCurrentListChanged(previousList, commitCallback)
                }
                return@launch
            }

            // Full diffing process
            val oldList = list ?: return@launch
            withContext(backgroundDispatcher) {
                val result = calculateDiff(oldList, newList)
                latchList(newList, result, commitCallback)
            }
        }
    }

    private suspend inline fun CoroutineScope.performWithRetry(action: () -> Unit) {
        var attempt = 0
        val maxAttempts = 3
        var lastException: Exception? = null

        while (isActive && attempt < maxAttempts) {
            try {
                action()
                return
            } catch (e: CancellationException) {
                // If the coroutine is cancelled, we exit immediately
                SceytLog.w(TAG, "Coroutine was cancelled during retry attempt $attempt", e)
                throw e
            } catch (ex: Exception) {
                lastException = ex
                SceytLog.w(TAG, "Catch Exception. Retry attempt $attempt", ex)
                attempt++
                if (attempt < maxAttempts) {
                    delay((attempt * 100L).coerceAtMost(500L)) // Exponential backoff
                }
            }
        }

        if (!isActive || lastException == null) return
        // If we've exhausted all retries, throw the last exception
        SceytLog.e(TAG, "Failed to perform action after $attempt attempts", lastException)
        throw lastException
    }

    private fun addItemsImpl(
            items: List<T>,
            position: Int = -1,
            commitCallback: (() -> Unit)? = null,
    ) = scope.launch {
        mutex.withLock {
            if (items.isEmpty()) return@withLock
            waitForSubmitJob()
            val previousList = readOnlyList
            val newList = list?.toMutableList() ?: mutableListOf()
            if (position == -1) {
                newList.addAll(items.toList())
            } else if (position in newList.indices) {
                newList.addAll(position, items.toList())
            }
            performWithRetry {
                list = newList
                readOnlyList = Collections.unmodifiableList(newList)
                val positionToInsert = position.takeIf { it != -1 } ?: previousList.size
                withContext(mainDispatcher) {
                    updateCallback.onInserted(positionToInsert, items.size)
                    onCurrentListChanged(previousList, commitCallback)
                }
            }
        }
    }.also { lastOperationsJob = it }

    private suspend fun waitForSubmitJob() {
        lastSubmitJob?.let { job ->
            // Suspend until the lastJob completes, whether it was canceled or succeeded
            if (!job.isCompleted) {
                suspendCancellableCoroutine { continuation ->
                    job.invokeOnCompletion { throwable ->
                        if (throwable == null || throwable is CancellationException) {
                            continuation.resume(Unit)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "AsyncListDiffer"
    }
}