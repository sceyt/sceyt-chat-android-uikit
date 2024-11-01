package com.sceyt.chatuikit.formatters

import android.content.Context
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser

typealias UserFormatter = Formatter<SceytUser>
typealias ChannelFormatter = Formatter<SceytChannel>

fun interface Formatter<T> {
    fun format(context: Context, from: T): CharSequence
}

val NoFormatter = Formatter<Nothing> { _, _ -> "" }