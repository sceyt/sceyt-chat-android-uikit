package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters

import com.sceyt.chatuikit.data.models.messages.SceytReaction


sealed class ReactedUserItem {
    data class Item(
            val reaction: SceytReaction
    ) : ReactedUserItem()

    object LoadingMore : ReactedUserItem()
}