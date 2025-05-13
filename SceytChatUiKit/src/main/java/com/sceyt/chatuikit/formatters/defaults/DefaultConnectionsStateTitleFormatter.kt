package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chat.connectivity_change.NetworkMonitor
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultConnectionsStateTitleFormatter : Formatter<ConnectionState> {

    override fun format(context: Context, from: ConnectionState): CharSequence {
        return if (!NetworkMonitor.isOnline())
            context.getString(R.string.sceyt_waiting_for_network_title)
        else when (from) {
            ConnectionState.Failed, ConnectionState.Disconnected -> context.getString(R.string.sceyt_disconnected_title)
            ConnectionState.Reconnecting, ConnectionState.Connecting -> context.getString(R.string.sceyt_connecting_title)
            ConnectionState.Connected -> ""
        }
    }
}