package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.models.SyncNearMessagesResult

internal class MessageWindowSyncGuard {
    private val lock = Any()
    private var centeredSyncMessageId = 0L
    private var centeredSyncGeneration = 0L

    fun startCenteredSync(messageId: Long): Long = synchronized(lock) {
        centeredSyncMessageId = messageId
        ++centeredSyncGeneration
    }

    fun invalidateCenteredSync() = synchronized(lock) {
        centeredSyncMessageId = 0L
        centeredSyncGeneration++
    }

    fun canEmitCenteredSyncResult(centerMessageId: Long, generation: Long): Boolean =
        synchronized(lock) {
            centerMessageId != 0L &&
                    centeredSyncMessageId == centerMessageId &&
                    centeredSyncGeneration == generation
        }

    fun canApplyCenteredSyncResult(
        centerMessageId: Long,
        generation: Long,
        topOffset: Int,
        isPaging: Boolean,
        isPreparingJump: Boolean,
    ): Boolean = synchronized(lock) {
        centerMessageId != 0L &&
                centeredSyncMessageId == centerMessageId &&
                centeredSyncGeneration == generation &&
                topOffset != -1 &&
                !isPaging &&
                !isPreparingJump
    }

    fun canAppendNewestSyncedMessages(
        hasNext: Boolean,
        hasNextDb: Boolean,
        isNewestSidePaging: Boolean,
    ): Boolean {
        return !hasNext && !hasNextDb && !isNewestSidePaging
    }
}

data class CenteredSyncMessagesResult(
    val generation: Long,
    val data: SyncNearMessagesResult,
)
