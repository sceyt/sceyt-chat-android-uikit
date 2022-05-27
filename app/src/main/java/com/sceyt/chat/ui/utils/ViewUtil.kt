package com.sceyt.chat.ui.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import kotlin.math.roundToInt

object ViewUtil {

    fun pxToDp(px: Float): Float {
        val densityDpi = Resources.getSystem().displayMetrics.densityDpi
        return px / (densityDpi / 160f)
    }

    fun dpToPx(dp: Float): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (dp * density).roundToInt()
    }

    fun expandHeight(v: View, from: Int = 0, duration: Long, endListener: (() -> Unit)? = null) {
        v.measure(
            View.MeasureSpec.makeMeasureSpec(v.rootView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(v.rootView.height, View.MeasureSpec.AT_MOST)
        )
        val targetHeight = v.measuredHeight

        val heightAnimator = ValueAnimator.ofInt(from, targetHeight)
        heightAnimator.addUpdateListener { animation ->
            v.layoutParams.height = animation.animatedValue as Int
            v.requestLayout()
        }
        heightAnimator.doOnEnd {
            endListener?.invoke()
        }
        heightAnimator.duration = duration
        heightAnimator.start()
        v.isVisible = true
    }

    fun expandHeightUnspecified(v: View, duration: Long) {
        v.measure(
            View.MeasureSpec.makeMeasureSpec(v.rootView.width, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(v.rootView.height, View.MeasureSpec.UNSPECIFIED)
        )
        val targetWidth = v.measuredHeight

        val widthAnimator = ValueAnimator.ofInt(0, targetWidth)
        widthAnimator.addUpdateListener { animation ->
            v.layoutParams.height = animation.animatedValue as Int
            v.requestLayout()
        }
        widthAnimator.duration = duration
        widthAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                v.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                v.requestLayout()
            }
        })
        widthAnimator.start()
    }

    fun collapseHeight(v: View, duration: Long, endListener: (() -> Unit)? = null) {
        val initialHeight = v.measuredHeight
        val heightAnimator = ValueAnimator.ofInt(0, initialHeight)
        heightAnimator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            v.layoutParams.height = initialHeight - animatedValue
            v.requestLayout()
        }
        heightAnimator.doOnEnd {
            endListener?.invoke()
        }
        heightAnimator.duration = duration
        heightAnimator.start()
    }


    fun expandWidth(v: View, duration: Long) {
        v.measure(
            View.MeasureSpec.makeMeasureSpec(v.rootView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(v.rootView.height, View.MeasureSpec.EXACTLY)
        )
        val targetWidth = v.measuredWidth

        val widthAnimator = ValueAnimator.ofInt(0, targetWidth)
        widthAnimator.addUpdateListener { animation ->
            v.layoutParams.width = animation.animatedValue as Int
            v.requestLayout()
        }
        widthAnimator.duration = 200
        widthAnimator.start()
    }

    fun expandWidthUnspecified(v: View, duration: Long) {
        v.measure(
            View.MeasureSpec.makeMeasureSpec(v.rootView.width, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(v.rootView.height, View.MeasureSpec.UNSPECIFIED)
        )
        val targetWidth = v.measuredWidth

        val widthAnimator = ValueAnimator.ofInt(0, targetWidth)
        widthAnimator.addUpdateListener { animation ->
            v.layoutParams.width = animation.animatedValue as Int
            v.requestLayout()
        }
        widthAnimator.duration = duration
        widthAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                v.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                v.requestLayout()
            }
        })
        widthAnimator.start()
    }

    fun collapseWidth(v: View, duration: Long) {
        val initialWidth = v.measuredHeight
        val widthAnimator = ValueAnimator.ofInt(0, initialWidth)
        widthAnimator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            v.layoutParams.width = initialWidth - animatedValue
            v.requestLayout()
        }
        widthAnimator.duration = duration
        widthAnimator.start()
    }

    fun getDisplayHeight(activity: Activity): Int {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result += context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}