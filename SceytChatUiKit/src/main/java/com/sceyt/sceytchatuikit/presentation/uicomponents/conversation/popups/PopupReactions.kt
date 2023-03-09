package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.popups

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
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.ReactionData
import com.sceyt.sceytchatuikit.databinding.SceytPopupAddReactionBinding
import com.sceyt.sceytchatuikit.extensions.screenWidthPx
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import java.lang.Integer.max

class PopupReactions(private var context: Context) : PopupWindow(context) {
    private lateinit var binding: SceytPopupAddReactionBinding
    private val defaultClickListener: PopupReactionsAdapter.OnItemClickListener by lazy { initClickListener() }
    private var clickListener: PopupReactionsAdapter.OnItemClickListener? = null

    fun showPopup(anchorView: View, message: SceytMessage, clickListener: PopupReactionsAdapter.OnItemClickListener): PopupReactions {
        this.clickListener = clickListener

        val reversed = !message.incoming
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val y = location[1]

        binding = SceytPopupAddReactionBinding.inflate(LayoutInflater.from(context))

        contentView = SceytPopupAddReactionBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }.root

        animationStyle = if (reversed) R.style.SceytReactionPopupAnimationReversed else R.style.SceytReactionPopupAnimationNormal
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isOutsideTouchable = true
        isFocusable = false
        setAdapter(reversed, message, defaultClickListener)

        with(binding.cardView) {
            measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            val minYPos = measuredHeight + marginBottom + marginTop
            val xPos = if (reversed) context.screenWidthPx() else 0
            val yPos = max(minYPos, y - measuredHeight - marginBottom)

            showAtLocation(anchorView, Gravity.NO_GRAVITY, xPos, yPos)
        }
        return this
    }

    private fun setAdapter(reversed: Boolean, message: SceytMessage, clickListener: PopupReactionsAdapter.OnItemClickListener) {
        val reactions = SceytKitConfig.fastReactions.map {
            val containsSelf = message.selfReactions?.map { reaction -> reaction.key }?.contains(it) == true
            ReactionItem.Reaction(ReactionData(it, containsSelf = containsSelf), message)
        } + ReactionItem.Other(message)
        val adapter = PopupReactionsAdapter(reactions, clickListener)
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
}