package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import com.sceyt.sceytchatuikit.data.models.messages.SceytReaction


sealed class ReactedUserItem {
    data class Item(
            val reaction: SceytReaction
    ) : ReactedUserItem()

    object LoadingMore : ReactedUserItem()
}