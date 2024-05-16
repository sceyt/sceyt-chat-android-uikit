package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytReactionTotal(
        var key: String,
        var score: Int = 1,
        var containsSelf: Boolean = false) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return ((other as? SceytReactionTotal) ?: return true).run {
            key == this@SceytReactionTotal.key && score == this@SceytReactionTotal.score
                    && containsSelf == this@SceytReactionTotal.containsSelf
        }
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + containsSelf.hashCode()
        return result
    }
}