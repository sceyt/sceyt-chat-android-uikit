package com.sceyt.chatuikit.presentation.common

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getString

class ExoCustomPlayPauseImage @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {

    @SuppressLint("PrivateResource")
    override fun setContentDescription(contentDescription: CharSequence?) {
        super.setContentDescription(contentDescription)
        when (contentDescription) {
            getString(androidx.media3.ui.R.string.exo_controls_play_description) -> setImageResource(R.drawable.sceyt_ic_play)
            getString(androidx.media3.ui.R.string.exo_controls_pause_description) -> setImageResource(R.drawable.sceyt_ic_pause)
        }
    }
}