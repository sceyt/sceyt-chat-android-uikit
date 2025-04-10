package com.sceyt.chatuikit.shared.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import kotlin.math.max

object ViewUtil {

    fun expandHeight(v: View, from: Int = 0, duration: Long,
                     updateListener: ((Int) -> Unit)? = null,
                     endListener: (() -> Unit)? = null): ValueAnimator {
        v.measure(
            View.MeasureSpec.makeMeasureSpec(v.rootView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(v.rootView.height, View.MeasureSpec.AT_MOST)
        )
        val targetHeight = v.measuredHeight

        val heightAnimator = ValueAnimator.ofInt(from, targetHeight)
        heightAnimator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            v.layoutParams.height = animatedValue
            v.requestLayout()
            updateListener?.invoke(animatedValue)
        }
        heightAnimator.doOnEnd {
            endListener?.invoke()
        }
        heightAnimator.duration = duration
        heightAnimator.start()
        v.isVisible = true
        return heightAnimator
    }

    fun expandHeightUnspecified(v: View, from: Int, duration: Long): ValueAnimator? {
        v.measure(
            View.MeasureSpec.makeMeasureSpec(v.rootView.width, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(v.rootView.height, View.MeasureSpec.UNSPECIFIED)
        )
        val targetWidth = v.measuredHeight

        val animator = ValueAnimator.ofInt(from, targetWidth)
        animator.addUpdateListener { animation ->
            v.layoutParams.height = animation.animatedValue as Int
            v.requestLayout()
        }
        animator.duration = duration
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                v.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                v.requestLayout()
            }
        })
        animator.start()
        return animator
    }

    fun collapseHeight(v: View, duration: Long, to: Int = 0,
                       updateListener: ((Int) -> Unit)? = null,
                       endListener: (() -> Unit)? = null): ValueAnimator {
        val initialHeight = v.measuredHeight
        val heightAnimator = ValueAnimator.ofInt(to, initialHeight)
        heightAnimator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            val height = max(to, initialHeight - animatedValue)
            v.layoutParams.height = height
            v.requestLayout()
            updateListener?.invoke(height)
        }
        heightAnimator.doOnEnd {
            endListener?.invoke()
        }
        heightAnimator.duration = duration
        heightAnimator.start()
        return heightAnimator
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
        widthAnimator.duration = duration
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
}