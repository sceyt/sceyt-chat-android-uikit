package com.sceyt.sceytchatuikit.presentation.common

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.extensions.screenPortraitWidthPx
import kotlin.math.min

class MaxWidthRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RecyclerView(context, attrs, defStyleAttr) {
    private var bubbleMaxWidth = (context.screenPortraitWidthPx() * 0.77f).toInt()

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        var width = MeasureSpec.getSize(widthSpec)
        val acceptableW = bubbleMaxWidth
        if (width > acceptableW) {
            width = acceptableW
            setMeasuredDimension(min(measuredWidth, width), measuredHeight)
        }
    }
}