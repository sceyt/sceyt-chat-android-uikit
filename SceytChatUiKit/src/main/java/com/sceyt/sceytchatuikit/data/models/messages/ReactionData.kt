package com.sceyt.sceytchatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReactionData(
        var key: String,
        val score: Long = 1,
        val containsSelf: Boolean = false) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return ((other as? ReactionData) ?: return true).run {
            key == this@ReactionData.key && score == this@ReactionData.score
                    && containsSelf == this@ReactionData.containsSelf
        }
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + containsSelf.hashCode()
        return result
    }
}