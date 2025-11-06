package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PendingVoteData(
        val pollId: String,
        val messageTid: Long,
        val optionId: String,
        val createdAt: Long,
        val isAdd: Boolean,
        val user: SceytUser,
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return other is PendingVoteData &&
                other.messageTid == messageTid &&
                other.pollId == pollId && other.optionId == optionId
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

