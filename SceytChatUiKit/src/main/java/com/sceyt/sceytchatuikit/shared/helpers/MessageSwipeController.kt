package com.sceyt.sceytchatuikit.shared.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.screenWidthPx
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class MessageSwipeController(private val context: Context, private val swipeControllerActions: SwipeControllerActions) : Callback() {

    private var imageDrawable: Drawable? = null
    private lateinit var shareRound: Drawable
    private var currentItemViewHolder: RecyclerView.ViewHolder? = null
    private lateinit var mView: View
    private var swipeBack = false

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        mView = viewHolder.itemView
        imageDrawable = context.getCompatDrawable(MessagesStyle.swipeReplyIcon)
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

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {


        if ((viewHolder as? BaseMsgViewHolder)?.enableReply != true) return

        if (actionState == ACTION_STATE_SWIPE) {
            setTouchListener(recyclerView, viewHolder)
        }
        val newDx = max(0f, dX - dX * 0.4f)
        super.onChildDraw(c, recyclerView, viewHolder, newDx, dY, actionState, isCurrentlyActive)
        currentItemViewHolder = viewHolder
        drawReplyButton(c, dX)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack = event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
            if (swipeBack) {
                if (abs(mView.translationX) >= context.screenWidthPx() * 0.18)
                    swipeControllerActions.showReplyUI(viewHolder.bindingAdapterPosition)
            }
            false
        }
    }

    private fun drawReplyButton(canvas: Canvas, dX: Float) {
        if (currentItemViewHolder == null) {
            return
        }
        val scale = min(1f, dX * 0.003f)
        val alpha = min(255f, 255 * scale).toInt()

        shareRound.alpha = alpha
        imageDrawable?.alpha = alpha

        val stopScalingX = dpToPx(130f)

        val x: Int = if (mView.translationX > stopScalingX) {
            stopScalingX / 2
        } else {
            (mView.translationX / 2).toInt()
        }

        val y = (mView.top + mView.measuredHeight / 2).toFloat()

        val roundSize = dpToPx(15f) * scale
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
}
