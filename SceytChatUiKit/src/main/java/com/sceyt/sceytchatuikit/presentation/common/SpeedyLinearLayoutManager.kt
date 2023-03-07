package com.sceyt.sceytchatuikit.presentation.common

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView


class SpeedyLinearLayoutManager : LinearLayoutManager {
    private val millisecondsPerInch = 25f //default is 25f (bigger = slower)
    private var customMillisecondsPerInch = 0f
    private var mPendingTargetPos = -1
    private var mPendingPosOffset = -1

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

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            if (mPendingTargetPos != -1 && (state?.itemCount ?: 0) > 0) {
                /*
                Data is present now, we can set the real scroll position
                */
                scrollToPositionWithOffset(mPendingTargetPos, mPendingPosOffset)
                mPendingTargetPos = -1
                mPendingPosOffset = -1
            }
            super.onLayoutChildren(recycler, state)
        } catch (_: Exception) {
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        /*
        May be needed depending on your implementation.
        Ignore target start position if InstanceState is available (page existed before already, keep position that user scrolled to)
         */
        mPendingTargetPos = -1
        mPendingPosOffset = -1
        super.onRestoreInstanceState(state)
    }

    /**
     * Sets a start position that will be used **as soon as data is available**.
     * May be used if your Adapter starts with itemCount=0 (async data loading) but you need to
     * set the start position already at this time. As soon as itemCount > 0,
     * it will set the scrollPosition, so that given itemPosition is visible.
     * @param position
     * @param offset
     */
    fun setTargetStartPos(position: Int, offset: Int) {
        mPendingTargetPos = position
        mPendingPosOffset = offset
    }

    fun smoothScrollToPositionWithDuration(recyclerView: RecyclerView, position: Int, millisecondsPerInch: Float) {
        customMillisecondsPerInch = millisecondsPerInch
        recyclerView.smoothScrollToPosition(position)
    }
}