package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getPresentableNameCheckDeleted
import com.sceyt.chatuikit.formatters.Formatter

data object DefaultUserNameFormatter : Formatter<SceytUser> {
    override fun format(context: Context, from: SceytUser): String {
        return from.getPresentableNameCheckDeleted(context)
    }
}

