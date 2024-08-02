package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.reactions

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.presentation.common.SelectableItem
import kotlinx.parcelize.Parcelize

sealed class ReactionItem : SelectableItem(), Parcelable {
    @Parcelize
    data class Reaction(val reaction: SceytReactionTotal,
                        val messageTid: Long,
                        val isPending: Boolean) : ReactionItem()

    @Parcelize
    data class Other(val message: SceytMessage) : ReactionItem()
}