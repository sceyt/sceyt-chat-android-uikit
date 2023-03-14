package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import com.sceyt.chat.models.message.Reaction


sealed class ReactedUserItem {
    data class Item(
            val reaction: Reaction
    ) : ReactedUserItem()

    object LoadingMore : ReactedUserItem()
}