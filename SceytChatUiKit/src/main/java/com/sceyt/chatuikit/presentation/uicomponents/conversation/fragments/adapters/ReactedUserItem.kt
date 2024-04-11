package com.sceyt.chatuikit.presentation.uicomponents.conversation.fragments.adapters

import com.sceyt.chatuikit.data.models.messages.SceytReaction


sealed class ReactedUserItem {
    data class Item(
            val reaction: SceytReaction
    ) : ReactedUserItem()

    object LoadingMore : ReactedUserItem()
}