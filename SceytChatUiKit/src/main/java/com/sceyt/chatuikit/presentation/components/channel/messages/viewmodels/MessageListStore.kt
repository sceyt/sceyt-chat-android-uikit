package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.comporators.MessageItemComparator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single owner of the rendered message list: the [MessageListState] snapshot,
 * the [MessageListRenderEffect] stream and the reducer-backed list mutations that
 * used to live inside [MessageListViewModel].
 *
 * The store is mapper-agnostic — it operates on already-mapped [MessageListItem]s.
 * Building items from [SceytMessage]s (which needs channel/user context) stays in the
 * view model. Multi-step pagination/sync sequences serialize through [withMutation].
 */
internal class MessageListStore(
    private val reducer: MessageListItemsReducer = MessageListItemsReducer(),
    private val recoveryScope: CoroutineScope? = null,
    private val recoveryDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private val _state = MutableStateFlow(MessageListState())
    val state = _state.asStateFlow()

    private val _renderEffects = MutableSharedFlow<MessageListRenderEffect>(
        extraBufferCapacity = 64
    )
    val renderEffects = _renderEffects.asSharedFlow()

    private val mutationMutex = Mutex()

    var enableDateSeparator = true

    val items: List<MessageListItem> get() = _state.value.items

    fun messageItems(): List<SceytMessage> =
        items.filterIsInstance<MessageItem>().map { it.message }

    fun lastMessageItem(): MessageItem? =
        items.lastOrNull { it is MessageItem } as? MessageItem

    fun messageItemsCount(items: List<MessageListItem> = this.items): Int =
        items.count { it is MessageItem }

    suspend fun <T> withMutation(block: suspend () -> T): T = mutationMutex.withLock { block() }

    fun emitEffect(effect: MessageListRenderEffect) {
        if (_renderEffects.tryEmit(effect)) return

        // Buffer overflow: the consumer fell behind. Drop the incremental effect and resync the
        // whole list instead, so the rendered list can't silently diverge from committed state.
        val recovery = MessageListRenderEffect.Replace(items = _state.value.items, force = false)
        if (_renderEffects.tryEmit(recovery)) return
        recoveryScope?.launch(recoveryDispatcher) { _renderEffects.emit(recovery) }
    }

    fun reset() {
        _state.value = MessageListState()
    }

    private fun commit(
        items: List<MessageListItem>,
        hasLoadedInitialMessages: Boolean = true,
    ) {
        _state.update { current ->
            current.copy(
                items = items,
                revision = current.revision + 1,
                hasLoadedInitialMessages = current.hasLoadedInitialMessages || hasLoadedInitialMessages
            )
        }
    }

    /**
     * Read-compute-commit-emit as one atomic step. [mutation] runs inside [MutableStateFlow.update],
     * so it always sees the latest committed list and any render effects it builds carry indices that
     * match exactly what gets committed — even when another coroutine mutates concurrently (CAS retries
     * the block). Effects are emitted once, after the commit, from the winning run.
     *
     * [mutation] MUST be side-effect free: CAS may invoke it several times before one wins.
     */
    private inline fun <T> mutate(
        crossinline mutation: (current: List<MessageListItem>) -> MutationResult<T>,
    ): T {
        var mutationResult: MutationResult<T>? = null
        _state.update { current ->
            val result = mutation(current.items)
            mutationResult = result
            if (!result.changed) current
            else current.copy(
                items = result.items,
                revision = current.revision + 1,
                hasLoadedInitialMessages = true
            )
        }
        val committedResult = requireNotNull(mutationResult)
        committedResult.effects.forEach(::emitEffect)
        return committedResult.value
    }

    fun replace(items: List<MessageListItem>, force: Boolean) {
        val normalized = reducer.normalize(items, enableDateSeparator)
        commit(normalized)
        emitEffect(MessageListRenderEffect.Replace(items = normalized, force = force))
    }

    fun addPrevPage(newItems: List<MessageListItem>) = mutate { current ->
        val merged = reducer.mergePrevPage(
            current = current,
            newItems = newItems,
            enableDateSeparator = enableDateSeparator
        )
        if (merged == current) {
            MutationResult(
                items = current,
                changed = false,
                effects = listOf(MessageListRenderEffect.HideLoadingPrev),
                value = Unit
            )
        } else {
            MutationResult(
                items = merged,
                changed = true,
                effects = listOf(MessageListRenderEffect.PrependPage(resultItems = merged)),
                value = Unit
            )
        }
    }

    fun addNextPage(newItems: List<MessageListItem>) = mutate { current ->
        val result = reducer.appendNextPage(current = current, newItems = newItems)
        when {
            !result.changed -> MutationResult(current, changed = false, effects = emptyList(), value = Unit)
            result.insertedItems.isEmpty() -> MutationResult(
                items = result.resultItems,
                changed = true,
                effects = listOf(MessageListRenderEffect.HideLoadingNext),
                value = Unit
            )

            else -> MutationResult(
                items = result.resultItems,
                changed = true,
                effects = listOf(MessageListRenderEffect.AppendPage(resultItems = result.insertedItems)),
                value = Unit
            )
        }
    }

    fun addRealtime(newItems: List<MessageListItem>, isOutgoing: Boolean): Boolean = mutate { current ->
        val result = reducer.appendRealtime(current = current, newItems = newItems)
        if (!result.changed) {
            MutationResult(current, changed = false, effects = emptyList(), value = false)
        } else {
            MutationResult(
                items = result.resultItems,
                changed = true,
                effects = listOf(
                    MessageListRenderEffect.AppendRealtime(
                        items = result.insertedItems,
                        scroll = if (isOutgoing) AppendRealtimeScroll.Always else AppendRealtimeScroll.IfAtEnd
                    )
                ),
                value = true
            )
        }
    }

    /** Drops items matching [predicate] and commits the result (unconditionally, like the original). */
    fun removeItems(predicate: (MessageListItem) -> Boolean) = mutate { current ->
        MutationResult(
            items = current.filterNot(predicate),
            changed = true,
            effects = emptyList(),
            value = Unit
        )
    }

    fun deleteByTids(tids: List<Long>): DeleteOutcome = mutate { current ->
        val result = reducer.deleteByTids(current, tids.toSet(), enableDateSeparator)
        if (!result.changed) {
            MutationResult(current, changed = false, effects = emptyList(), value = DeleteOutcome(changed = false, isEmpty = false))
        } else {
            // A reassigned day header can't be expressed by an incremental delete; fall back to Replace.
            val effect = if (result.canUseIncrementalDelete) {
                MessageListRenderEffect.DeleteTids(tids)
            } else {
                MessageListRenderEffect.Replace(result.resultItems, force = false)
            }
            MutationResult(
                items = result.resultItems,
                changed = true,
                effects = listOf(effect),
                value = DeleteOutcome(changed = true, isEmpty = messageItemsCount(result.resultItems) == 0)
            )
        }
    }

    fun clear() {
        commit(emptyList())
        emitEffect(MessageListRenderEffect.Clear)
    }

    fun deleteAllBefore(predicate: (MessageListItem) -> Boolean) = mutate { current ->
        val updated = current.filterNot(predicate)
        if (updated == current) {
            MutationResult(current, changed = false, effects = emptyList(), value = Unit)
        } else {
            MutationResult(
                items = updated,
                changed = true,
                effects = listOf(MessageListRenderEffect.Replace(updated, force = false)),
                value = Unit
            )
        }
    }

    fun setBodyExpanded(messageTid: Long): Boolean = mutate { current ->
        var changed = false
        val updated = current.map { item ->
            if (item is MessageItem && item.message.tid == messageTid) {
                changed = true
                item.copy(message = item.message.copy(isBodyExpanded = true))
            } else item
        }
        MutationResult(items = updated, changed = changed, effects = emptyList(), value = changed)
    }

    fun updateItem(
        predicate: (MessageItem) -> Boolean,
        diff: MessageDiff? = MessageDiff.DEFAULT_FALSE,
        diffProvider: ((MessageItem, MessageItem) -> MessageDiff?)? = null,
        notifyVisibleOnly: Boolean = false,
        notify: Boolean = true,
        update: (MessageItem) -> MessageItem,
    ): Boolean = mutate { current ->
        val index = current.indexOfFirst { it is MessageItem && predicate(it) }
        if (index == -1) {
            MutationResult(current, changed = false, effects = emptyList(), value = false)
        } else {
            val oldItem = current[index] as MessageItem
            val updatedItem = update(oldItem)
            if (updatedItem === oldItem) {
                MutationResult(current, changed = false, effects = emptyList(), value = true)
            } else {
                val updated = current.toMutableList()
                updated[index] = updatedItem
                MutationResult(
                    items = updated,
                    changed = true,
                    effects = listOf(
                        MessageListRenderEffect.UpdateItem(
                            index = index,
                            item = updatedItem,
                            diff = if (diffProvider != null) diffProvider(oldItem, updatedItem) else diff,
                            notifyVisibleOnly = notifyVisibleOnly,
                            notify = notify
                        )
                    ),
                    value = true
                )
            }
        }
    }

    fun updateAllItems(
        predicate: (MessageItem) -> Boolean,
        diff: MessageDiff? = MessageDiff.DEFAULT_FALSE,
        diffProvider: ((MessageItem, MessageItem) -> MessageDiff?)? = null,
        notifyVisibleOnly: Boolean = false,
        update: (MessageItem) -> MessageItem,
    ) = mutate { current ->
        var changed = false
        val effects = mutableListOf<MessageListRenderEffect>()
        val updated = current.mapIndexed { index, item ->
            if (item is MessageItem && predicate(item)) {
                val updatedItem = update(item)
                if (updatedItem !== item) {
                    changed = true
                    effects.add(
                        MessageListRenderEffect.UpdateItem(
                            index = index,
                            item = updatedItem,
                            diff = if (diffProvider != null) diffProvider(item, updatedItem) else diff,
                            notifyVisibleOnly = notifyVisibleOnly
                        )
                    )
                    updatedItem
                } else item
            } else item
        }
        MutationResult(items = updated, changed = changed, effects = effects, value = Unit)
    }

    /**
     * Merges synced messages into the list as one atomic step: drops loading items, appends the new
     * items, sorts, de-duplicates message items by tid and normalizes date separators — then emits a
     * single [MessageListRenderEffect.Sort]. Collapsing this into one mutation (instead of append + sort)
     * prevents a sorted-but-not-normalized intermediate state that could leave duplicate same-day
     * separators. Returns true if the list changed.
     */
    fun mergeSynced(newItems: List<MessageListItem>): Boolean = mutate { current ->
        val withoutLoading = current.filterNot { it is MessageListItem.LoadingNextItem }
        val sorted = (withoutLoading + newItems).sortedWith(MessageItemComparator())

        val seenTids = HashSet<Long>()
        val deduped = sorted.filter { item ->
            if (item is MessageItem) seenTids.add(item.message.tid) else true
        }
        val normalized = reducer.normalize(deduped, enableDateSeparator)

        if (normalized == current) {
            MutationResult(current, changed = false, effects = emptyList(), value = false)
        } else {
            MutationResult(
                items = normalized,
                changed = true,
                effects = listOf(MessageListRenderEffect.Sort(normalized)),
                value = true
            )
        }
    }

    fun mergeAroundCenter(centerMessageId: Long, newItems: List<MessageListItem>): Boolean = mutate { current ->
        val merged = reducer.mergeAroundCenter(
            current = current,
            newItems = newItems,
            centerMessageId = centerMessageId,
            enableDateSeparator = enableDateSeparator
        )
        when (merged) {
            null -> MutationResult(current, changed = false, effects = emptyList(), value = false)
            current -> MutationResult(current, changed = false, effects = emptyList(), value = false)

            else -> MutationResult(
                items = merged,
                changed = true,
                effects = listOf(MessageListRenderEffect.Replace(merged, force = false)),
                value = true
            )
        }
    }

    fun sort() = mutate { current ->
        val sorted = current.sortedWith(MessageItemComparator())
        if (sorted == current) {
            MutationResult(current, changed = false, effects = emptyList(), value = Unit)
        } else {
            MutationResult(
                items = sorted,
                changed = true,
                effects = listOf(MessageListRenderEffect.Sort(sorted)),
                value = Unit
            )
        }
    }

    data class DeleteOutcome(val changed: Boolean, val isEmpty: Boolean)

    /** Outcome of a [mutate] step: the next list, whether it changed, the effects to emit and a return value. */
    private data class MutationResult<T>(
        val items: List<MessageListItem>,
        val changed: Boolean,
        val effects: List<MessageListRenderEffect>,
        val value: T,
    )
}
