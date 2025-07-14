package com.sceyt.chatuikit.shared.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.ItemTouchHelper.Callback
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.screenWidthPx
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import kotlin.math.abs
import kotlin.math.min


class MessageSwipeController(
        context: Context,
        private val style: MessageItemStyle,
        private val swipeControllerActions: SwipeControllerActions,
) : Callback() {

    private var imageDrawable: Drawable? = null
    private lateinit var shareRound: Drawable
    private var currentItemViewHolder: RecyclerView.ViewHolder? = null
    private lateinit var mView: View
    private var swipeBack = false
    private var dX: Float = 0f
    private var maxAcceptableExpand = context.screenWidthPx() * 0.22
    private var enableSwipe: Boolean = true
    private var hasTriggeredHapticFeedback = false

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        mView = viewHolder.itemView
        imageDrawable = style.swipeToReplyIcon
        shareRound = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor("#3D000000".toColorInt())
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

    override fun onChildDraw(
            c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
            dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean,
    ) {


        if ((viewHolder as? BaseMessageViewHolder)?.enableReply != true || !enableSwipe) return

        if (actionState == ACTION_STATE_SWIPE) {
            setTouchListener(recyclerView, viewHolder)
        }

        var newDx: Float = dX
        // if swipe more than maxAcceptableExpand, then slow down the swipe
        if (dX > maxAcceptableExpand) {
            val diff = dX - this.dX
            newDx = this.dX + diff * 0.3f
        } else
            this.dX = dX

        super.onChildDraw(c, recyclerView, viewHolder, newDx, dY, actionState, isCurrentlyActive)
        currentItemViewHolder = viewHolder
        drawReplyButton(c, dX)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack = event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
            val richThreshold = abs(mView.translationX) >= maxAcceptableExpand * 0.85
            if (swipeBack && richThreshold) {
                swipeControllerActions.showReplyUI(viewHolder.bindingAdapterPosition)
            }

            if (!hasTriggeredHapticFeedback) {
                if (richThreshold) {
                    mView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    hasTriggeredHapticFeedback = true
                }
            } else if (!richThreshold) {
                hasTriggeredHapticFeedback = false
            }
            false
        }
    }

    private fun drawReplyButton(canvas: Canvas, dX: Float) {
        if (currentItemViewHolder == null) {
            return
        }
        val scale = min(1f, dX * 0.0045f)
        val alpha = min(255f, 255 * scale).toInt()

        shareRound.alpha = alpha
        imageDrawable?.alpha = alpha

        val x: Int = if (dX > maxAcceptableExpand) {
            (maxAcceptableExpand / 2).toInt()
        } else {
            (dX / 2).toInt()
        }

        val y = (mView.top + mView.measuredHeight / 2).toFloat()

        val roundSize = dpToPx(16f) * scale
        shareRound.setBounds(
            (x - roundSize).toInt(),
            (y - roundSize).toInt(),
            (x + roundSize).toInt(),
            (y + roundSize).toInt()
        )
        shareRound.draw(canvas)

        imageDrawable?.setBounds(
            (x - dpToPx(12f) * scale).toInt(),
            (y - dpToPx(11f) * scale).toInt(),
            (x + dpToPx(12f) * scale).toInt(),
            (y + dpToPx(10f) * scale).toInt()
        )
        imageDrawable?.draw(canvas)
    }

    fun setSwipeEnabled(enable: Boolean) {
        enableSwipe = enable
    }
}
