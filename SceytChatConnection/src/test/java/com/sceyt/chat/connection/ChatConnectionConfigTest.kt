package com.sceyt.chat.connection

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ChatConnectionConfigTest {

    @Test
    fun negativeTokenExpirationLeewayIsRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ChatConnectionConfig(tokenExpirationLeewaySeconds = -1L)
        }

        assertThat(error).hasMessageThat()
            .isEqualTo("tokenExpirationLeewaySeconds must not be negative")
    }
}
