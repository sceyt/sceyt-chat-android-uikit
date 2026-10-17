package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MessageWindowSyncGuardTest {

    @Test
    fun `centered sync result must match active generation and visible start message`() {
        val guard = MessageWindowSyncGuard()
        val firstGeneration = guard.startCenteredSync(100)

        assertThat(guard.canEmitCenteredSyncResult(100, firstGeneration)).isTrue()
        assertThat(
            guard.canApplyCenteredSyncResult(
                centerMessageId = 100,
                generation = firstGeneration,
                topOffset = 12,
                isPaging = false,
                isPreparingJump = false
            )
        ).isTrue()

        assertThat(
            guard.canApplyCenteredSyncResult(
                centerMessageId = 101,
                generation = firstGeneration,
                topOffset = 12,
                isPaging = false,
                isPreparingJump = false
            )
        ).isFalse()
        assertThat(
            guard.canApplyCenteredSyncResult(
                centerMessageId = 100,
                generation = firstGeneration + 1,
                topOffset = 12,
                isPaging = false,
                isPreparingJump = false
            )
        ).isFalse()
        assertThat(
            guard.canApplyCenteredSyncResult(
                centerMessageId = 100,
                generation = firstGeneration,
                topOffset = -1,
                isPaging = false,
                isPreparingJump = false
            )
        ).isFalse()
        assertThat(
            guard.canApplyCenteredSyncResult(
                centerMessageId = 100,
                generation = firstGeneration,
                topOffset = 12,
                isPaging = true,
                isPreparingJump = false
            )
        ).isFalse()
        assertThat(
            guard.canApplyCenteredSyncResult(
                centerMessageId = 100,
                generation = firstGeneration,
                topOffset = 12,
                isPaging = false,
                isPreparingJump = true
            )
        ).isFalse()
    }

    @Test
    fun `centered sync generation rejects stale same-message response`() {
        val guard = MessageWindowSyncGuard()
        val staleGeneration = guard.startCenteredSync(100)
        val activeGeneration = guard.startCenteredSync(100)

        assertThat(guard.canEmitCenteredSyncResult(100, staleGeneration)).isFalse()
        assertThat(guard.canEmitCenteredSyncResult(100, activeGeneration)).isTrue()

        guard.invalidateCenteredSync()

        assertThat(guard.canEmitCenteredSyncResult(100, activeGeneration)).isFalse()
    }

    @Test
    fun `newest synced messages append only when newest side is complete and idle`() {
        val guard = MessageWindowSyncGuard()

        assertThat(
            guard.canAppendNewestSyncedMessages(
                hasNext = false,
                hasNextDb = false,
                isNewestSidePaging = false
            )
        ).isTrue()
        assertThat(
            guard.canAppendNewestSyncedMessages(
                hasNext = true,
                hasNextDb = false,
                isNewestSidePaging = false
            )
        ).isFalse()
        assertThat(
            guard.canAppendNewestSyncedMessages(
                hasNext = false,
                hasNextDb = true,
                isNewestSidePaging = false
            )
        ).isFalse()
        assertThat(
            guard.canAppendNewestSyncedMessages(
                hasNext = false,
                hasNextDb = false,
                isNewestSidePaging = true
            )
        ).isFalse()
    }
}
