package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.comporators

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

class MessageComparator : Comparator<SceytMessage> {

    override fun compare(next: SceytMessage, prev: SceytMessage): Int {
        return when {
            prev.deliveryStatus == DeliveryStatus.Pending || next.deliveryStatus == DeliveryStatus.Pending -> 0

            else -> {
                next.createdAt.compareTo(prev.createdAt)
            }
        }
    }
}