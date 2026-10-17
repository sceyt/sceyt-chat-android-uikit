package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinDb
import com.sceyt.chatuikit.persistence.mappers.mergePins
import org.junit.Test

class MergePinsTest {

    private fun confirmed(tid: Long) = sceytPinnedMessage(id = tid, message = sceytMessage(id = tid, tid = tid))

    private fun pending(tid: Long, isPin: Boolean) = PendingPinDb(
        pendingPin = pendingPin(messageTid = tid, isPin = isPin),
        message = messageDb(messageEntity(id = tid, tid = tid)),
    )

    @Test
    fun `a pending unpin hides its confirmed pin`() {
        val merged = mergePins(listOf(confirmed(1L), confirmed(2L)), listOf(pending(1L, isPin = false)))
        assertThat(merged.map { it.messageTid }).containsExactly(2L)
    }

    @Test
    fun `a pending pin is appended after the confirmed pins`() {
        val merged = mergePins(listOf(confirmed(1L)), listOf(pending(3L, isPin = true)))
        assertThat(merged.map { it.messageTid }).containsExactly(1L, 3L).inOrder()
        assertThat(merged.last().id).isEqualTo(0L)
    }

    @Test
    fun `a confirmed pin keeps its place while its pending pin is being settled`() {
        val merged = mergePins(listOf(confirmed(1L), confirmed(2L)), listOf(pending(1L, isPin = true)))
        assertThat(merged.map { it.messageTid }).containsExactly(1L, 2L).inOrder()
    }

    @Test
    fun `a pending unpin without a confirmed pin shows nothing`() {
        assertThat(mergePins(emptyList(), listOf(pending(1L, isPin = false)))).isEmpty()
    }
}
