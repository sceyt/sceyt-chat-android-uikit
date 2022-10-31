package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.comporators

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

class MessageComparator : Comparator<SceytMessage> {

    override fun compare(next: SceytMessage, prev: SceytMessage): Int {
        return next.createdAt.compareTo(prev.createdAt)
    }
}