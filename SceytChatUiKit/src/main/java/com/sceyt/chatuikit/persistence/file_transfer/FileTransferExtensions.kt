package com.sceyt.chatuikit.persistence.file_transfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

/** UIKit supports one media/file attachment per message; link previews are not file transfers. */
internal val SceytAttachment.transferKey: String
    get() = messageTid.toString()
