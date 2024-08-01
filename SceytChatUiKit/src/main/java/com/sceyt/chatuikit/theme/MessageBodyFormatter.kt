package com.sceyt.chatuikit.theme

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytMessage

fun interface MessageBodyFormatter {
    fun format(context: Context, message: SceytMessage): CharSequence
}