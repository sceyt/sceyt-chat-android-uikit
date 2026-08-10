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

    private val reducer = MessageListItemsReducer(enableDateSeparator)
    private val mutationLock = Any()
    private val _state = MutableStateFlow(Snapshot())
    val state = _state.asStateFlow()

    fun replace(items: List<MessageListItem>) = mutate { current ->
        reducer.replace(items).let {
            if (reducer.hasSameItemInstances(current, it)) current else it
        }
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

    /** The callback runs once while state is locked; return the original item for a no-op. */
    fun updateByTid(tid: Long, update: (MessageItem) -> MessageItem) = mutate { current ->
        reducer.updateByTid(current, tid, update)
    }

    fun deleteByTids(tids: Set<Long>) = mutate { current ->
        reducer.deleteByTids(current, tids)
    }

    fun mergeAroundCenter(centerMessageId: Long, items: List<MessageListItem>) = mutate { current ->
        reducer.mergeAroundCenter(current, items, centerMessageId)
    }

    fun clear() = mutate { current -> if (current.isEmpty()) current else emptyList() }

    private inline fun mutate(reduce: (List<MessageListItem>) -> List<MessageListItem>) {
        synchronized(mutationLock) {
            val current = _state.value
            val nextItems = reduce(current.items)
            if (nextItems !== current.items) {
                _state.value = Snapshot(items = nextItems, revision = current.revision + 1)
            }
        }
    }
}
