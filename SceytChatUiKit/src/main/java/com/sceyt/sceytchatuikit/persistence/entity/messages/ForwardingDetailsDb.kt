package com.sceyt.sceytchatuikit.persistence.entity.messages

data class ForwardingDetailsDb(
        val messageId: Long,
        val userId: String?,
        val hops: Int
)