package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Canonical, lifecycle-independent message-list state. It is not wired to the UI yet. */
internal class MessageListState(
    enableDateSeparator: Boolean,
) {
    data class Snapshot(
        val items: List<MessageListItem> = emptyList(),
        val revision: Long = 0,
    )

    data class MutationResult(
        val revision: Long,
        val changed: Boolean,
    )

    data class ItemUpdateResult(
        val revision: Long,
        val found: Boolean,
        val changed: Boolean,
    )

    private val reducer = MessageListItemsReducer(enableDateSeparator)
    private val mutationLock = Any()
    private val _state = MutableStateFlow(Snapshot())
    val state = _state.asStateFlow()

    fun replace(items: List<MessageListItem>) = mutate { current ->
        reducer.replaceWindow(current, items)
    }

    fun prependPage(items: List<MessageListItem>) = mutate { current ->
        reducer.prependPage(current, items)
    }

    fun appendPage(items: List<MessageListItem>) = mutate { current ->
        reducer.appendPage(current, items)
    }

    fun appendRealtime(items: List<MessageListItem>) = mutate { current ->
        reducer.appendRealtime(current, items)
    }

    fun hideLoadingPrev() = mutate(reducer::hideLoadingPrev)

    fun hideLoadingNext() = mutate(reducer::hideLoadingNext)

    fun removeUnreadSeparator() = mutate(reducer::removeUnreadSeparator)

    /** The callback runs once while state is locked; return the original item for a no-op. */
    fun updateByTid(tid: Long, update: (MessageItem) -> MessageItem): ItemUpdateResult {
        var found = false
        val result = mutate { current ->
            reducer.updateByTid(current, tid) { item ->
                found = true
                update(item)
            }
        }
        return ItemUpdateResult(
            revision = result.revision,
            found = found,
            changed = result.changed,
        )
    }

    fun deleteByTids(tids: Set<Long>) = mutate { current ->
        reducer.deleteByTids(current, tids)
    }

    fun deleteAtOrBeforePreservingPending(createdAt: Long) = mutate { current ->
        reducer.deleteAtOrBeforePreservingPending(current, createdAt)
    }

    fun clearSelection() = mutate(reducer::clearSelection)

    fun reconcileMessages(
        rowOnlyUpdates: List<MessageItem>,
        rowAndReplyUpdates: List<MessageItem>,
    ) = mutate { current ->
        reducer.reconcileMessages(current, rowOnlyUpdates, rowAndReplyUpdates)
    }

    fun mergeAroundCenter(centerMessageId: Long, items: List<MessageListItem>) = mutate { current ->
        reducer.mergeAroundCenter(current, items, centerMessageId)
    }

    fun clear() = mutate { current -> if (current.isEmpty()) current else emptyList() }

    private inline fun mutate(
        reduce: (List<MessageListItem>) -> List<MessageListItem>,
    ): MutationResult {
        return synchronized(mutationLock) {
            val current = _state.value
            val nextItems = reduce(current.items)
            if (nextItems !== current.items) {
                val next = Snapshot(items = nextItems, revision = current.revision + 1)
                _state.value = next
                MutationResult(revision = next.revision, changed = true)
            } else {
                MutationResult(revision = current.revision, changed = false)
            }
        }
    }
}
