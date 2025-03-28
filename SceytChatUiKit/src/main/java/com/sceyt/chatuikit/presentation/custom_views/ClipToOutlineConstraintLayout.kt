package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

class ClipToOutlineConstraintLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        clipToOutline = true
    }
}