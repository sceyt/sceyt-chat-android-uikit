package com.sceyt.chatuikit.presentation.uicomponents.conversation.popups

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.PopupWindow
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.databinding.SceytPopupAddReactionBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isRtl
import com.sceyt.chatuikit.extensions.marginHorizontal
import com.sceyt.chatuikit.extensions.screenWidthPx
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import java.lang.Integer.max
import kotlin.math.min

class PopupReactions(private var context: Context) : PopupWindow(context) {
    private lateinit var binding: SceytPopupAddReactionBinding
    private val defaultClickListener: PopupReactionsAdapter.OnItemClickListener by lazy { initClickListener() }
    private var clickListener: PopupReactionsAdapter.OnItemClickListener? = null

    fun showPopup(anchorView: View, message: SceytMessage,
                  reactions: List<String> = SceytChatUIKit.theme.defaultReactions,
                  clickListener: PopupReactionsAdapter.OnItemClickListener): PopupReactions {
        this.clickListener = clickListener

        val reversed = !message.incoming && !context.isRtl()
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val y = location[1]

        contentView = SceytPopupAddReactionBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }.root

        binding.applyStyle()

        animationStyle = if (reversed) R.style.SceytReactionPopupAnimationReversed else R.style.SceytReactionPopupAnimationNormal
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isOutsideTouchable = true
        isFocusable = false
        setAdapter(reversed, message, reactions, defaultClickListener)

        with(binding.cardView) {
            measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            this@PopupReactions.width = min(context.screenWidthPx(), measuredWidth + marginHorizontal)

            val minYPos = measuredHeight + marginBottom + marginTop
            val xPos = if (reversed) context.screenWidthPx() else 0
            val yPos = max(minYPos, y - measuredHeight - marginBottom)

            showAtLocation(anchorView, Gravity.NO_GRAVITY, xPos, yPos)
        }

        return this
    }

    private fun setAdapter(reversed: Boolean, message: SceytMessage, reactions: List<String>,
                           clickListener: PopupReactionsAdapter.OnItemClickListener) {
        val reactionsItems = reactions.map {
            val reactionItem = message.messageReactions?.find { data -> data.reaction.key == it }
            val containsSelf = reactionItem?.reaction?.containsSelf ?: false
            ReactionItem.Reaction(SceytReactionTotal(it, containsSelf = containsSelf), message.tid, reactionItem?.isPending
                    ?: false)
        }.run {
            if ((message.userReactions?.size ?: 0) < SceytChatUIKit.config.maxSelfReactionsSize)
                plus(ReactionItem.Other(message))
            else this
        }
        val adapter = PopupReactionsAdapter(reactionsItems, clickListener)
        binding.rvEmoji.adapter = adapter
        binding.rvEmoji.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.sceyt_layout_animation_linear_scale).apply {
            order = if (reversed) LayoutAnimationController.ORDER_REVERSE else LayoutAnimationController.ORDER_NORMAL
        }
    }

    private fun initClickListener(): PopupReactionsAdapter.OnItemClickListener {
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

    private fun SceytPopupAddReactionBinding.applyStyle() {
        cardView.setCardBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.backgroundColorSections))
    }
}