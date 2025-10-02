package com.sceyt.chatuikit.presentation.components.channel.messages.popups

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.databinding.SceytPopupAddReactionBinding
import com.sceyt.chatuikit.extensions.isRtl
import com.sceyt.chatuikit.extensions.marginHorizontal
import com.sceyt.chatuikit.extensions.screenWidthPx
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.styles.messages_list.ReactionPickerStyle
import java.lang.Integer.max
import kotlin.math.min

open class ReactionsPopup(
        private var context: Context,
) : PopupWindow(context) {
    private lateinit var binding: SceytPopupAddReactionBinding
    private val defaultClickListener: PopupReactionsAdapter.OnItemClickListener by lazy {
        initClickListener()
    }
    private var clickListener: PopupReactionsAdapter.OnItemClickListener? = null

    protected open fun initAndShow(
            anchorView: View,
            message: SceytMessage,
            reactions: List<String>,
            style: ReactionPickerStyle,
            clickListener: PopupReactionsAdapter.OnItemClickListener,
    ): ReactionsPopup {
        this.clickListener = clickListener

        val reversed = !message.incoming && !context.isRtl()
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val y = location[1]

        contentView = SceytPopupAddReactionBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }.root

        binding.applyStyle(style)

        animationStyle = if (reversed) R.style.SceytReactionPopupAnimationReversed else R.style.SceytReactionPopupAnimationNormal
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        isOutsideTouchable = true
        isFocusable = false
        setAdapter(reversed, message, reactions, style, defaultClickListener)

        with(binding.cardView) {
            measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            this@ReactionsPopup.width = min(context.screenWidthPx(), measuredWidth + marginHorizontal)

            val minYPos = measuredHeight + marginBottom + marginTop
            val xPos = if (reversed) context.screenWidthPx() else 0
            val yPos = max(minYPos, y - measuredHeight - marginBottom)

            showAtLocation(anchorView, Gravity.NO_GRAVITY, xPos, yPos)
        }

        return this
    }

    protected open fun setAdapter(
            reversed: Boolean,
            message: SceytMessage,
            reactions: List<String>,
            style: ReactionPickerStyle,
            clickListener: PopupReactionsAdapter.OnItemClickListener,
    ) {
        val reactionsItems = reactions.map {
            val reactionItem = message.messageReactions?.find { data -> data.reaction.key == it }
            val containsSelf = reactionItem?.reaction?.containsSelf ?: false
            ReactionItem.Reaction(SceytReactionTotal(it, containsSelf = containsSelf), message.tid, reactionItem?.isPending
                    ?: false)
        }.run {
            if ((message.userReactions?.size
                            ?: 0) < SceytChatUIKit.config.messageReactionPerUserLimit)
                plus(ReactionItem.Other(message))
            else this
        }
        val adapter = PopupReactionsAdapter(reactionsItems, style, clickListener)
        binding.rvEmoji.adapter = adapter
        binding.rvEmoji.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.sceyt_layout_animation_linear_scale).apply {
            order = if (reversed) LayoutAnimationController.ORDER_REVERSE else LayoutAnimationController.ORDER_NORMAL
        }
    }

    protected open fun initClickListener(): PopupReactionsAdapter.OnItemClickListener {
        return object : PopupReactionsAdapter.OnItemClickListener {
            override fun onReactionClick(reaction: ReactionItem.Reaction) {
                dismiss()
                clickListener?.onReactionClick(reaction)
            }

            override fun onAddClick() {
                dismiss()
                clickListener?.onAddClick()
            }
        }
    }

    protected open fun SceytPopupAddReactionBinding.applyStyle(style: ReactionPickerStyle) {
        cardView.setCardBackgroundColor(style.backgroundColor)
    }

    companion object {

        fun showPopup(
                anchorView: View,
                message: SceytMessage,
                reactions: List<String>,
                style: ReactionPickerStyle,
                clickListener: PopupReactionsAdapter.OnItemClickListener,
        ): ReactionsPopup {
            return ReactionsPopup(anchorView.context)
                .initAndShow(anchorView, message, reactions, style, clickListener)
        }
    }
}