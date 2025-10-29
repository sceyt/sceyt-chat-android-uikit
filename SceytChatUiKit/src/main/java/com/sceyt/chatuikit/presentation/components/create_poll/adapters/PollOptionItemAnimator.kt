package com.sceyt.chatuikit.presentation.components.create_poll.adapters

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class PollOptionItemAnimator : DefaultItemAnimator() {
    private val runningAnimators = mutableMapOf<RecyclerView.ViewHolder, Animator>()

    init {
        addDuration = 350
        removeDuration = 250
        moveDuration = 250
        changeDuration = 250
        supportsChangeAnimations = false
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        holder.itemView.scaleY = 0.85f
        holder.itemView.scaleX = 0.95f

        holder.itemView.post {
            dispatchAddStarting(holder)

            val scaleXAnimator = ObjectAnimator.ofFloat(holder.itemView, View.SCALE_X, 0.95f, 1f)
            val scaleYAnimator = ObjectAnimator.ofFloat(holder.itemView, View.SCALE_Y, 0.85f, 1f)

            val animatorSet = AnimatorSet().apply {
                playTogether(scaleXAnimator, scaleYAnimator)
                duration = addDuration
                interpolator = OvershootInterpolator(0.5f)
                doOnEnd {
                    runningAnimators.remove(holder)
                    holder.itemView.scaleX = 1f
                    holder.itemView.scaleY = 1f
                    dispatchAddFinished(holder)
                }
            }

            runningAnimators[holder] = animatorSet
            animatorSet.start()
        }

        return false
    }

    override fun endAnimation(holder: RecyclerView.ViewHolder) {
        // Cancel any running animator for this holder
        runningAnimators.remove(holder)?.cancel()

        // Reset view state - important for view recycling
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f

        super.endAnimation(holder)
    }

    override fun endAnimations() {
        // Cancel all running animations
        runningAnimators.values.forEach { it.cancel() }
        runningAnimators.clear()
        super.endAnimations()
    }
}

