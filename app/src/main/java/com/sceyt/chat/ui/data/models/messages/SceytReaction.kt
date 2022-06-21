package com.sceyt.chat.ui.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytReaction(
        var key: String,
        val score: Long = 1,
        val containsSelf: Boolean = false) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return ((other as? SceytReaction) ?: return true).run {
            key == this@SceytReaction.key && score == this@SceytReaction.score
                    && containsSelf == this@SceytReaction.containsSelf
        }
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + containsSelf.hashCode()
        return result
    }
}