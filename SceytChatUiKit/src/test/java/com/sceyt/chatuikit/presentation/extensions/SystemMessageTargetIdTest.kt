package com.sceyt.chatuikit.presentation.extensions

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.PinnedMessageMetadata
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SystemMessageAction
import com.sceyt.chatuikit.persistence.logicimpl.usecases.sceytMessage
import org.junit.Test

/**
 * What the pin announcement points at, which is also what decides whether tapping it does
 * anything at all.
 */
class SystemMessageTargetIdTest {

    @Test
    fun `resolves the parent id`() {
        val announcement = pinAnnouncement(parent = sceytMessage(id = PINNED_ID))

        assertThat(announcement.systemMessageTargetId()).isEqualTo(PINNED_ID)
    }

    @Test
    fun `falls back to the metadata id before the server echo populates the parent`() {
        val announcement = pinAnnouncement(parent = null)

        assertThat(announcement.systemMessageTargetId()).isEqualTo(PINNED_ID)
    }

    @Test
    fun `points at nothing once the pinned message is deleted`() {
        val announcement = pinAnnouncement(
            parent = sceytMessage(id = PINNED_ID, state = MessageState.Deleted)
        )

        // The metadata id is still there, so the fallback must not resurrect the jump.
        assertThat(announcement.systemMessageTargetId()).isNull()
    }

    @Test
    fun `other system messages point at nothing`() {
        val announcement = pinAnnouncement(parent = sceytMessage(id = PINNED_ID))
            .copy(body = SystemMessageAction.MemberAdded.value)

        assertThat(announcement.systemMessageTargetId()).isNull()
    }

    private fun pinAnnouncement(parent: SceytMessage?) = sceytMessage(id = 900L, tid = 900L).copy(
        body = SystemMessageAction.PinMessage.value,
        type = "system",
        metadata = Gson().toJson(PinnedMessageMetadata(PINNED_ID.toString())),
        parentMessage = parent,
    )

    private companion object {
        const val PINNED_ID = 42L
    }
}
