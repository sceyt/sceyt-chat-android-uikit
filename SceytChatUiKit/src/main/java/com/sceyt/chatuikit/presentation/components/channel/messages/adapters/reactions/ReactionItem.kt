package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.ReactionData
import com.sceyt.chatuikit.presentation.common.SelectableItem
import kotlinx.parcelize.Parcelize

sealed class ReactionItem : SelectableItem(), Parcelable {
    @Parcelize
    data class Reaction(
            val reaction: ReactionData,
            val messageTid: Long,
            val isPending: Boolean
    ) : ReactionItem()

    @Parcelize
    data class Other(val message: SceytMessage) : ReactionItem()
}