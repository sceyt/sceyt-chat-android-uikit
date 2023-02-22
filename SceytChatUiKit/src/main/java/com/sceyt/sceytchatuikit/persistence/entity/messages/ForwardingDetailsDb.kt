package com.sceyt.sceytchatuikit.persistence.entity.messages

data class ForwardingDetailsDb(
        var messageId: Long,
        val userId: String?,
        var hops: Int
)