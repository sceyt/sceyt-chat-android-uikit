package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.formatters.Formatter

class DefaultAvatarInitialsFormatter : Formatter<String> {
    //todo
    override fun format(context: Context, from: String): String {
        return from
    }
}

