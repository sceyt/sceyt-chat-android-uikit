package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.message.PinDetails.PinType
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytPinnedMessage(
    val id: Long,
    val channelId: Long,
    val messageId: Long,
    val messageTid: Long,
    val scope: PinType,
    val pinnedAt: Long?,
    val pinnedUntil: Long?,
    val pinnedBy: SceytUser?,
    val message: SceytMessage,
    val syncState: PinSyncState,
    val retryCount: Int,
) : Parcelable {

    val isPending: Boolean get() = messageId == 0L

    val isExpired: Boolean
        get() = pinnedUntil?.let { it != 0L && it <= System.currentTimeMillis() } == true

}

enum class PinSyncState(val value: Int) {
    Unspecified(PinSyncStates.UNSPECIFIED),
    Synced(PinSyncStates.SYNCED),
    PendingPin(PinSyncStates.PENDING_PIN),
    PendingUnpin(PinSyncStates.PENDING_UNPIN);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: Unspecified
    }
}

internal object PinSyncStates {
    const val UNSPECIFIED = 0
    const val SYNCED = 1
    const val PENDING_PIN = 2
    const val PENDING_UNPIN = 3
}