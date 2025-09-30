package com.sceyt.chatuikit.data.models.channels

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DraftVoiceAttachment(
        val channelId: Long,
        val filePath: String,
        val duration: Int,
        val amplitudes: List<Int>,
) : Parcelable
