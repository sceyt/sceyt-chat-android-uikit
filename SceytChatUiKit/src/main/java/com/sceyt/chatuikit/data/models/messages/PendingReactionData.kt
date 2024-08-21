package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.vanniktech.ui.Parcelize

@Parcelize
data class PendingReactionData(
        val messageId: Long,
        val key: String,
        val score: Int,
        val count: Long,
        val createdAt: Long,
        val isAdd: Boolean,
        val incomingMsg: Boolean
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return other is PendingReactionData && other.messageId == messageId && other.key == key
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}