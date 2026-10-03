package com.sceyt.chatuikit.persistence.differs

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.SceytPinDetails
import org.junit.Test

class MessageDifferPinTest {

    @Test
    fun `adding or removing a pin is a content change`() {
        val unpinned = createMessage(createdAt = 1L, id = 1L, tid = 1L)
        val pinned = unpinned.copy(
            pinDetails = SceytPinDetails(
                isPinned = true,
                pinnedTill = 0L,
                pinType = PinType.SHARED,
            )
        )

        assertThat(pinned.diff(unpinned).pinChanged).isTrue()
        assertThat(unpinned.diffContent(pinned).pinChanged).isTrue()
    }

    @Test
    fun `an unchanged pin does not trigger a redundant rebind`() {
        val pinned = createMessage(createdAt = 1L, id = 1L, tid = 1L).copy(
            pinDetails = SceytPinDetails(
                isPinned = true,
                pinnedTill = 0L,
                pinType = PinType.PERSONAL,
            )
        )

        assertThat(pinned.diff(pinned.copy()).pinChanged).isFalse()
    }
}
