package com.sceyt.chatuikit.extensions

import android.widget.ImageView
import com.sceyt.chatuikit.R

fun progressToMediaPlayerPosition(progress: Float, mediaDuration: Long): Long {
    return ((progress * mediaDuration) / 100f).toLong()
}

fun mediaPlayerPositionToSeekBarProgress(currentPosition: Long, mediaDuration: Long): Float {
    return (currentPosition * 100f / mediaDuration)
}