package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.database.entity.messages.PinSyncStateEntity
import com.sceyt.chatuikit.persistence.mappers.toParentMessageEntity
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import org.junit.Test

class ParentMessagePinMappingTest {
    @Test
    fun `parent pin state comes from the durable relation even without a mirror`() {
        val parent = sceytMessage().toParentMessageEntity().copy(
            pinnedMessage = pinnedEntity(syncState = PinSyncStateEntity.Synced.value)
        )
        assertThat(parent.messageEntity.pinDetails).isNull()
        assertThat(parent.toSceytMessage().pinDetails?.isPinned).isTrue()
    }

    @Test
    fun `a parent with a pending unpin is not marked pinned`() {
        val parent = sceytMessage().toParentMessageEntity().copy(
            pinnedMessage = pinnedEntity(syncState = PinSyncStateEntity.PendingUnpin.value)
        )
        assertThat(parent.toSceytMessage().pinDetails).isNull()
    }
}
