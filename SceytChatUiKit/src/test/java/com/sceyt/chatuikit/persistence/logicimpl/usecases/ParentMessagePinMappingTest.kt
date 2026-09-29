package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.mappers.toParentMessageEntity
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import org.junit.Test

class ParentMessagePinMappingTest {
    @Test
    fun `parent pin state comes from the confirmed pin relation`() {
        val parent = sceytMessage().toParentMessageEntity().copy(pinnedMessage = pinnedEntity())
        assertThat(parent.toSceytMessage().pinDetails?.isPinned).isTrue()
    }

    @Test
    fun `a parent with a pending unpin is not marked pinned`() {
        val parent = sceytMessage().toParentMessageEntity().copy(
            pinnedMessage = pinnedEntity(),
            pendingPin = pendingPin(isPin = false),
        )
        assertThat(parent.toSceytMessage().pinDetails).isNull()
    }

    @Test
    fun `a parent with a pending pin is marked pinned before the server confirms`() {
        val parent = sceytMessage().toParentMessageEntity().copy(pendingPin = pendingPin())
        assertThat(parent.toSceytMessage().pinDetails?.isPinned).isTrue()
    }
}
