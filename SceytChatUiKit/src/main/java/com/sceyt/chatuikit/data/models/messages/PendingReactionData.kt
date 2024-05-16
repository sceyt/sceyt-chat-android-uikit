package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.vanniktech.ui.Parcelize

@Parcelize
data class PendingReactionData(
        val messageId: Long,
        var key: String,
        var score: Int,
        var count: Long,
        var createdAt: Long,
        var isAdd: Boolean,
        var incomingMsg: Boolean
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return other is PendingReactionData && other.messageId == messageId && other.key == key
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}