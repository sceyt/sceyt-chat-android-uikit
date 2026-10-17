package com.sceyt.chatuikit.presentation.components.channel.messages

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges the gap between a message status update (e.g. Pending -> Sent for a forwarded message)
 * and the message list UI.
 *
 * A status update can arrive while its target message is momentarily absent from the visible list
 * — e.g. the adapter is asynchronously rebuilding its backing list. Such an update would otherwise
 * be lost, leaving an already-sent message stuck on "Pending".
 *
 * [onStatusUpdate] applies the update immediately if the message is present, otherwise parks it.
 * [reconcile] re-applies parked updates and should be called whenever the list commits a change,
 * i.e. exactly when a previously-missing message can become present again.
 */
internal class PendingMessageStatusReconciler {

    private val parked = ConcurrentHashMap<Long, SceytMessage>()

    val parkedCount: Int get() = parked.size

    /**
     * Applies [message] to the list via [applyToList]; if the message is not found there
     * (applyToList returns false), parks the update keyed by tid for a later [reconcile].
     */
    fun onStatusUpdate(message: SceytMessage, applyToList: (SceytMessage) -> Boolean) {
        if (!applyToList(message))
            parked[message.tid] = message
    }

    /**
     * Retries every parked update via [applyToList], dropping the ones that are now applied.
     * A still-missing update stays parked for the next commit.
     */
    fun reconcile(applyToList: (SceytMessage) -> Boolean) {
        val iterator = parked.entries.iterator()
        while (iterator.hasNext()) {
            if (applyToList(iterator.next().value))
                iterator.remove()
        }
    }

    /** Removes and returns the parked update for [tid], if any — used to render the latest state. */
    fun take(tid: Long): SceytMessage? = parked.remove(tid)
}
