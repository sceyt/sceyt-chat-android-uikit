package com.sceyt.sceytchatuikit.extensions

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.core.view.isVisible
import androidx.transition.Transition

fun View.changeAlphaWithAnimation(fromAlpha: Float, toAlpha: Float, animDuration: Long, endListener: () -> Unit = {}) {
    animation?.cancel()
    animation = AlphaAnimation(fromAlpha, toAlpha).apply {
        duration = animDuration
        setAnimationListener(animationListener(onAnimationEnd = {
            endListener.invoke()
        }))
        start()
    }
}

fun View.changeAlphaWithAnim(toAlpha: Float, animDuration: Long, endListener: () -> Unit = {}) {
    animate().alpha(toAlpha)
        .setListener(animatorListener(onAnimationEnd = {
            endListener.invoke()
        }, onAnimationCancel = {
            endListener.invoke()
        }))
        .duration = animDuration
}


fun View.startScaleAnimOut(duration: Long, fromScaleX: Float = 0f, fromScaleY: Float = 0f, finishedListener: (() -> Unit)? = null) {
    val scaleDownX = ObjectAnimator.ofFloat(this, "scaleX", fromScaleX, 1f)
    val scaleDownY = ObjectAnimator.ofFloat(this, "scaleY", fromScaleY, 1f)
    scaleDownX.duration = duration
    scaleDownY.duration = duration

    AnimatorSet().apply {
        play(scaleDownX).with(scaleDownY)
        start()
        addListener(animatorListener(onAnimationEnd = {
            finishedListener?.invoke()
        }))
    }
}

fun View.startScaleAnim(duration: Long, isOut: Boolean = true,
                        fromScaleX: Float = if (isOut) 0f else 1f,
                        fromScaleY: Float = if (isOut) 0f else 1f,
                        finishedListener: (() -> Unit)? = null): AnimatorSet {
    val scaleDownX = ObjectAnimator.ofFloat(this, "scaleX", fromScaleX, if (isOut) 1f else 0f)
    val scaleDownY = ObjectAnimator.ofFloat(this, "scaleY", fromScaleY, if (isOut) 1f else 0f)
    scaleDownX.duration = duration
    scaleDownY.duration = duration

    return AnimatorSet().apply {
        play(scaleDownX).with(scaleDownY)
        start()
        addListener(animatorListener(onAnimationEnd = {
            finishedListener?.invoke()
        }))
    }
}


fun View.awaitAnimationEnd(animLister: Animation.AnimationListener? = null) {
    if (animation == null || animation.hasEnded())
        animLister?.onAnimationEnd(animation)
    else animation.setAnimationListener(animLister)
}

fun View.hasAnimation(): Boolean {
    return animation != null && !animation.hasEnded()
}

fun View.hasNotAnimation(): Boolean {
    return animation == null || !animation.hasStarted() || animation.hasEnded()
}

fun View.scaleViewOut(startScale: Float, endScale: Float, duration: Long = 200,
                      pivotX: Float = 0.5f, pivotY: Float = 0.5f,
                      finishedListener: ((Animation?) -> Unit) = { }) {
    val anim: Animation = ScaleAnimation(
        startScale, endScale,  // Start and end values for the X axis scaling
        startScale, endScale,  // Start and end values for the Y axis scaling
        Animation.RELATIVE_TO_SELF, pivotX,  // Pivot point of X scaling
        Animation.RELATIVE_TO_SELF, pivotY) // Pivot point of Y scaling
    anim.fillAfter = true // Needed to keep the result of the animation
    anim.duration = duration
    anim.setAnimationListener(animationListener(onAnimationEnd = finishedListener))
    startAnimation(anim)
}

fun View.scaleViewWithAnim(startScale: Float, endScale: Float, duration: Long = 200,
                           pivotX: Float = 0.5f, pivotY: Float = 0.5f,
                           finishedListener: ((Animation?) -> Unit) = { }) {
    val anim: Animation = ScaleAnimation(
        startScale, endScale,  // Start and end values for the X axis scaling
        startScale, endScale,  // Start and end values for the Y axis scaling
        Animation.RELATIVE_TO_SELF, pivotX,  // Pivot point of X scaling
        Animation.RELATIVE_TO_SELF, pivotY) // Pivot point of Y scaling
    anim.fillAfter = true // Needed to keep the result of the animation
    anim.duration = duration
    anim.setAnimationListener(animationListener(onAnimationEnd = finishedListener))
    startAnimation(anim)
}

fun View.scaleAndAlphaAnim(startScale: Float, endScale: Float, duration: Long = 200, finishedListener: ((Animation?) -> Unit) = { }): AnimationSet {
    val startAlpha = if (endScale < 1) 1.0f else 0f
    val endAlpha = if (endScale < 1) 0f else 1f

    val animationSet = AnimationSet(true)
    val scaleAnimation = ScaleAnimation(startScale, endScale, startScale, endScale,
        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
    scaleAnimation.duration = duration
    animationSet.addAnimation(scaleAnimation)

    val alphaAnimation = AlphaAnimation(startAlpha, endAlpha)
    alphaAnimation.duration = (duration / 1.5).toLong()
    animationSet.addAnimation(alphaAnimation)
    animationSet.setAnimationListener(animationListener(onAnimationEnd = finishedListener))
    startAnimation(animationSet)
    return animationSet
}

fun View.visibleGoneWithScaleAnim(visible: Boolean) {
    if (hasNotAnimation()) {
        isClickable = false
        if (visible) {
            if (!isVisible) {
                isVisible = true
                scaleViewOut(0f, 1f) {
                    isClickable = true
                }
            } else isClickable = true
        } else {
            if (isVisible) {
                scaleViewWithAnim(1f, 0f) {
                    isVisible = false
                }
            }
        }
    }
}

fun View.visibleWithScaleAnim() {
    if (hasNotAnimation()) {
        if (!isVisible) {
            isVisible = true
            scaleViewOut(0f, 1f) {

            }
        }
    }
}

inline fun animationListener(
        crossinline onAnimationRepeat: (animation: Animation?) -> Unit = { _ -> },
        crossinline onAnimationStart: (animation: Animation?) -> Unit = { _ -> },
        crossinline onAnimationEnd: (animation: Animation?) -> Unit = { _ -> }): Animation.AnimationListener {
    return object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {
            onAnimationRepeat.invoke(animation)
        }

        override fun onAnimationEnd(animation: Animation?) {
            onAnimationEnd.invoke(animation)
        }

        override fun onAnimationStart(animation: Animation?) {
            onAnimationStart.invoke(animation)
        }
    }
}

inline fun animatorListener(
        crossinline onAnimationRepeat: (animation: Animator?) -> Unit = { _ -> },
        crossinline onAnimationEnd: (animation: Animator?) -> Unit = { _ -> },
        crossinline onAnimationCancel: (animation: Animator?) -> Unit = { _ -> },
        crossinline onAnimationStart: (animation: Animator?) -> Unit = { _ -> }): Animator.AnimatorListener {
    return object : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator) {
            onAnimationRepeat.invoke(animation)
        }

        override fun onAnimationEnd(animation: Animator) {
            onAnimationEnd.invoke(animation)
        }

        override fun onAnimationCancel(animation: Animator) {
            onAnimationCancel.invoke(animation)
        }

        override fun onAnimationStart(animation: Animator) {
            onAnimationStart.invoke(animation)
        }

    }
}

inline fun transitionListener(
        crossinline onTransitionStart: (transition: Transition) -> Unit = { _ -> },
        crossinline onTransitionResume: (transition: Transition) -> Unit = { _ -> },
        crossinline onTransitionPause: (transition: Transition) -> Unit = { _ -> },
        crossinline onTransitionCancel: (transition: Transition) -> Unit = { _ -> },
        crossinline onTransitionEnd: (transition: Transition) -> Unit = { _ -> }): Transition.TransitionListener {
    return object : Transition.TransitionListener {

        override fun onTransitionStart(transition: Transition) {
            onTransitionStart.invoke(transition)
        }

        override fun onTransitionResume(transition: Transition) {
            onTransitionResume(transition)
        }

        override fun onTransitionPause(transition: Transition) {
            onTransitionPause(transition)
        }

        override fun onTransitionCancel(transition: Transition) {
            onTransitionCancel(transition)
            onTransitionEnd.invoke(transition)
        }

        override fun onTransitionEnd(transition: Transition) {
            onTransitionEnd.invoke(transition)
        }
    }
}
