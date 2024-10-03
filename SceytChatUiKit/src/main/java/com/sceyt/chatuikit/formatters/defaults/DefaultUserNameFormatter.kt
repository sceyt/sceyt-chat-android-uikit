package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getPresentableNameWithYou
import com.sceyt.chatuikit.formatters.Formatter

data object DefaultUserNameFormatter : Formatter<SceytUser> {
    override fun format(context: Context, from: SceytUser): String {
        return from.getPresentableNameWithYou(context)
    }
}

