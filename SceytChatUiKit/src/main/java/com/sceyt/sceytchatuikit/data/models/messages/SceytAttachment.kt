package com.sceyt.sceytchatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytAttachment(
        val tid: Long,
        val name: String,
        val type: String,
        val metadata: String?,
        val fileSize: Long,
        val url: String
) : Parcelable, Cloneable {

    public override fun clone(): SceytAttachment {
        return SceytAttachment(tid, name, type, metadata, fileSize, url)
    }
}
