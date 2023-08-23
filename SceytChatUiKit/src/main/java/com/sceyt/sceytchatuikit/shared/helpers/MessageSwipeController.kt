package com.sceyt.sceytchatuikit.shared.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class MessageSwipeController(private val context: Context, private val swipeControllerActions: SwipeControllerActions) : Callback() {

    private var imageDrawable: Drawable? = null
    private lateinit var shareRound: Drawable

    private var currentItemViewHolder: RecyclerView.ViewHolder? = null
    private lateinit var mView: View
    private var dX = 0f

    private var replyButtonProgress: Float = 0.toFloat()
    private var lastReplyButtonAnimationTime: Long = 0
    private var swipeBack = false
    private var isVibrate = false
    private var startTracking = false

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        mView = viewHolder.itemView
        imageDrawable = context.getCompatDrawable(R.drawable.sceyt_is_reply_swipe)
        shareRound = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor("#2D000000".toColorInt())
        }

        return makeMovementFlags(ACTION_STATE_IDLE, RIGHT)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
        if (swipeBack) {
            swipeBack = false
            return 0
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {

        if (actionState == ACTION_STATE_SWIPE) {
            setTouchListener(recyclerView, viewHolder)
        }
        var newDx: Float = 0f
        /* if (mView.translationX < maxTracking || dX < this.dX) {
             super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
             this.dX = dX
             startTracking = true
         } else {
              newDx = this.dX + (dX * 0.08f)
             super.onChildDraw(c, recyclerView, viewHolder, newDx, dY, actionState, isCurrentlyActive)
             //this.dX = newDx
         }*/

        val dv = max(0f, dX - dX * 0.5f)
        super.onChildDraw(c, recyclerView, viewHolder, dv, dY, actionState, isCurrentlyActive)
        currentItemViewHolder = viewHolder
        drawReplyButton(c)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack = event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
            if (swipeBack) {
                if (abs(mView.translationX) >= this@MessageSwipeController.convertTodp(100)) {
                    swipeControllerActions.showReplyUI(viewHolder.bindingAdapterPosition)
                    Log.i("sfsdfsdfTAG", "setTouchListener: ")
                }
            }
            false
        }
    }

    private fun drawReplyButton(canvas: Canvas) {
        if (currentItemViewHolder == null) {
            return
        }
        val translationX = mView.translationX
        val newTime = System.currentTimeMillis()
        val dt = Math.min(17, newTime - lastReplyButtonAnimationTime)
        lastReplyButtonAnimationTime = newTime
        val showing = translationX >= convertTodp(30)
        if (showing) {
            if (replyButtonProgress < 1.0f) {
                replyButtonProgress += dt / 180.0f
                if (replyButtonProgress > 1.0f) {
                    replyButtonProgress = 1.0f
                } else {
                    mView.invalidate()
                }
            }
        } else if (translationX <= 0.0f) {
            replyButtonProgress = 0f
            startTracking = false
            isVibrate = false
        } else {
            if (replyButtonProgress > 0.0f) {
                replyButtonProgress -= dt / 180.0f
                if (replyButtonProgress < 0.1f) {
                    replyButtonProgress = 0f
                } else {
                    mView.invalidate()
                }
            }
        }
        val alpha: Int
        val scale: Float
        if (showing) {
            scale = if (replyButtonProgress <= 0.8f) {
                1.2f * (replyButtonProgress / 0.8f)
            } else {
                1.2f - 0.2f * ((replyButtonProgress - 0.8f) / 0.2f)
            }
            alpha = min(255f, 255 * (replyButtonProgress / 0.8f)).toInt()
        } else {
            scale = replyButtonProgress
            alpha = min(255f, 255 * replyButtonProgress).toInt()
        }
        shareRound.alpha = alpha

        imageDrawable?.alpha = alpha
        if (startTracking) {
            if (!isVibrate && mView.translationX >= convertTodp(100)) {
                mView.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
                isVibrate = true
            }
        }

        val x: Int = if (mView.translationX > convertTodp(130)) {
            convertTodp(130) / 2
        } else {
            (mView.translationX / 2).toInt()
        }

        val y = (mView.top + mView.measuredHeight / 2).toFloat()
        shareRound.colorFilter =
                PorterDuffColorFilter("#c4c4c4".toColorInt(), PorterDuff.Mode.MULTIPLY)

        shareRound.setBounds(
            (x - convertTodp(16) * scale).toInt(),
            (y - convertTodp(16) * scale).toInt(),
            (x + convertTodp(16) * scale).toInt(),
            (y + convertTodp(16) * scale).toInt()
        )
        shareRound.draw(canvas)
        imageDrawable?.setBounds(
            (x - convertTodp(12) * scale).toInt(),
            (y - convertTodp(11) * scale).toInt(),
            (x + convertTodp(12) * scale).toInt(),
            (y + convertTodp(10) * scale).toInt()
        )
        imageDrawable?.draw(canvas)
        shareRound.alpha = 255
        imageDrawable?.alpha = 255
    }

    private fun convertTodp(pixel: Int): Int {
        return dpToPx(pixel.toFloat())
    }
}
