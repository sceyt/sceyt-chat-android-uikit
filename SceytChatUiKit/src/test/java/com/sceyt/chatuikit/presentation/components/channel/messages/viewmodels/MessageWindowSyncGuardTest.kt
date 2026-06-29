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
    fun `synced messages append only when lower side is complete and idle`() {
        val guard = MessageWindowSyncGuard()

        assertThat(
            guard.canAppendSyncedMessages(
                hasNext = false,
                hasNextDb = false,
                isPaging = false
            )
        ).isTrue()
        assertThat(
            guard.canAppendSyncedMessages(
                hasNext = true,
                hasNextDb = false,
                isPaging = false
            )
        ).isFalse()
        assertThat(
            guard.canAppendSyncedMessages(
                hasNext = false,
                hasNextDb = true,
                isPaging = false
            )
        ).isFalse()
        assertThat(
            guard.canAppendSyncedMessages(
                hasNext = false,
                hasNextDb = false,
                isPaging = true
            )
        ).isFalse()
    }
}
