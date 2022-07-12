package com.sceyt.chat.ui.presentation.common

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class SpeedyLinearLayoutManager : LinearLayoutManager {
    private val millisecondsPerInch = 25f //default is 25f (bigger = slower)
    private var customMillisecondsPerInch = 0f

    constructor(context: Context?) : super(context)
    constructor(context: Context?, orientation: Int, reverseLayout: Boolean) : super(context, orientation, reverseLayout)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
        val linearSmoothScroller: LinearSmoothScroller = object : LinearSmoothScroller(recyclerView.context) {

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return if (customMillisecondsPerInch != 0f) {
                    val value = customMillisecondsPerInch / displayMetrics.densityDpi
                    customMillisecondsPerInch = 0f
                    value
                } else millisecondsPerInch / displayMetrics.densityDpi
            }
        }

        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }

    fun smoothScrollToPositionWithDuration(recyclerView: RecyclerView, position: Int, millisecondsPerInch: Float) {
        customMillisecondsPerInch = millisecondsPerInch
        recyclerView.smoothScrollToPosition(position)
    }
}