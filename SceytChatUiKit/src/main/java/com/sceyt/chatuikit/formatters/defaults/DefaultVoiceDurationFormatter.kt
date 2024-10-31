package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultVoiceDurationFormatter : Formatter<Long> {

    override fun format(context: Context, from: Long): CharSequence {
        return from.durationToMinSecShort()
    }
}