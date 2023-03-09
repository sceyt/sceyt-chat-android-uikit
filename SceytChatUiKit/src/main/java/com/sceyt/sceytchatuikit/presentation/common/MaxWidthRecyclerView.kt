package com.sceyt.sceytchatuikit.presentation.common

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

class MaxWidthRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RecyclerView(context, attrs, defStyleAttr) {
    private var bodyMaxWidth = context.resources.getDimensionPixelSize(com.sceyt.sceytchatuikit.R.dimen.bodyMaxWidth)

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        var width = MeasureSpec.getSize(widthSpec)
        if (width > bodyMaxWidth) {
            width = bodyMaxWidth
            setMeasuredDimension(min(measuredWidth, width), measuredHeight)
        }
    }
}