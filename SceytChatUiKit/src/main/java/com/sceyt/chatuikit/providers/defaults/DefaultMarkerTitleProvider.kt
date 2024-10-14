package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.providers.VisualProvider

open class DefaultMarkerTitleProvider : VisualProvider<MarkerType, String> {
    override fun provide(context: Context, from: MarkerType): String {
        return when (from) {
            MarkerType.Displayed -> context.getString(R.string.sceyt_seen_by)
            MarkerType.Received -> context.getString(R.string.sceyt_delivered_to)
            MarkerType.Played -> context.getString(R.string.sceyt_played_by)
            is MarkerType.Custom -> from.value
        }
    }
}