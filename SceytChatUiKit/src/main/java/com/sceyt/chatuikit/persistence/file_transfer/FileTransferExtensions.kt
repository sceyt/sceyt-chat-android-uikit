package com.sceyt.chatuikit.persistence.file_transfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

/** Uses the stable attachment ID, falling back to its opaque URL before an ID is available. */
internal val SceytAttachment.transferKey: String
    get() = id?.takeUnless { it == 0L }?.let { "$messageTid:id:$it" }
        ?: "$messageTid:url:${url.orEmpty()}"
