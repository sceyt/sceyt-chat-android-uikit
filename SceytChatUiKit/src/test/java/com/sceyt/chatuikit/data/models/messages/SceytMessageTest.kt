package com.sceyt.chatuikit.data.models.messages

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import org.junit.Test

class SceytMessageTest {

    @Test
    fun `hashCode follows tid identity`() {
        val first = createMessage(createdAt = 1, id = 1, tid = 10)
        val sameTid = createMessage(createdAt = 2, id = 2, tid = 10)
        val otherTid = createMessage(createdAt = 3, id = 3, tid = 11)

        assertThat(first).isEqualTo(sameTid)
        assertThat(first.hashCode()).isEqualTo(sameTid.hashCode())
        assertThat(first.hashCode()).isEqualTo(10L.hashCode())
        assertThat(first.hashCode()).isNotEqualTo(otherTid.hashCode())
    }
}
