package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.comporators

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage

class MessageComparator : Comparator<SceytMessage> {

    override fun compare(next: SceytMessage, prev: SceytMessage): Int {
        val nextIsPending = next.deliveryStatus == DeliveryStatus.Pending
        val prevIsPending = prev.deliveryStatus == DeliveryStatus.Pending
        
        // If one is pending and the other is not, pending goes to the bottom
        return when {
            nextIsPending && !prevIsPending -> 1  // next is pending, so it goes after prev
            !nextIsPending && prevIsPending -> -1 // prev is pending, so next goes before prev
            else -> next.createdAt.compareTo(prev.createdAt) // both have same pending state, sort by time
        }
    }
}