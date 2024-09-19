package com.sceyt.chatuikit.formatters

import android.content.Context

interface AvatarInitialsFormatter : Formatter<String> {
    override fun format(context: Context, from: String): String
}

class DefaultAvatarInitialsFormatter : AvatarInitialsFormatter {
    override fun format(context: Context, from: String): String {
        return from
    }
}

