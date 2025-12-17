package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.data.models.channels.SceytChannel

sealed interface CommonGroupClickListeners {

    fun interface ClickListener : CommonGroupClickListeners {
        fun onGroupClick(view: View, channel: SceytChannel)
    }
}