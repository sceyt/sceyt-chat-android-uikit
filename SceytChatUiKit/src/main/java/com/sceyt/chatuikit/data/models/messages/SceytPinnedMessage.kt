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

    companion object {
        const val UNKNOWN_SERVER_PIN_ID = Long.MAX_VALUE
    }
}

enum class PinSyncState {
    Unspecified,
    Synced,
    PendingPin,
    PendingUnpin,
}