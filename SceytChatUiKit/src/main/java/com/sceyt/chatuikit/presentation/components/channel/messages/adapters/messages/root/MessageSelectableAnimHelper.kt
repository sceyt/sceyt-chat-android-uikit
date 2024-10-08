package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.CheckBox
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.presentation.extensions.isPending
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessagesAdapter

class MessageSelectableAnimHelper(private var viewHolder: RecyclerView.ViewHolder) {
    private val checkBoxSize = dpToPx(22f)
    private var animation: ValueAnimator? = null

    fun doOnBind(view: View?, item: MessageListItem) {
        view ?: return
        val message = (item as? MessageListItem.MessageItem)?.message ?: return
        val isMultiSelectableMode = (viewHolder.bindingAdapter as? MessagesAdapter)?.isMultiSelectableMode()
                ?: false
        if (isMultiSelectableMode && !message.isPending()) {
            view.isVisible = true
            setCheckedState(view, message.isSelected)

            view.updateLayoutParams<MarginLayoutParams> {
                marginStart = checkBoxSize / 2
            }
        } else view.isVisible = false
    }

    fun doOnAttach(checkBox: View?, message: MessageListItem) {
        doOnBind(checkBox, message)
    }

    fun setSelectableState(view: View?, item: MessageListItem) {
        view ?: return
        val message = (item as? MessageListItem.MessageItem)?.message ?: return
        setCheckedState(view, message.isSelected)

        if (!view.isVisible)
            view.updateLayoutParams<MarginLayoutParams> {
                marginStart = -checkBoxSize
            }

        view.isVisible = true
        animation?.cancel()
        animation = ObjectAnimator.ofFloat(view.marginStart.toFloat(), checkBoxSize / 2f)
        animation?.addUpdateListener {
            view.updateLayoutParams<MarginLayoutParams> {
                marginStart = (it.animatedValue as Float).toInt()
            }
        }
        animation?.start()
    }

    fun cancelSelectableState(view: View?, onAnimEnd: (() -> Unit)? = null) {
        (view ?: return).isVisible = true
        animation?.cancel()
        animation = ObjectAnimator.ofFloat(view.marginStart.toFloat(), -checkBoxSize.toFloat())
        animation?.addUpdateListener {
            view.updateLayoutParams<MarginLayoutParams> {
                marginStart = (it.animatedValue as Float).toInt()
            }
        }
        animation?.addListener(onEnd = {
            if (view.marginStart == -checkBoxSize)
                view.isVisible = false
            onAnimEnd?.invoke()
        })
        animation?.start()
    }

    private fun setCheckedState(view: View, isChecked: Boolean) {
        if (view is CheckBox) {
            view.isChecked = isChecked
        } else view.isSelected = isChecked
    }
}