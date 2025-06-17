package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.user.PresenceState
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytPresence(
    val state: PresenceState,
    val status: String,
    val lastActiveAt: Long
) : Parcelable