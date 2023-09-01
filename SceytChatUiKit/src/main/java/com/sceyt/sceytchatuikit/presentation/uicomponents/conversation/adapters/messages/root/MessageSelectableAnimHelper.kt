package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root

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
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.presentation.common.isPending
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessagesAdapter

class MessageSelectableAnimHelper(private var viewHolder: RecyclerView.ViewHolder) {
    private val checkBoxSize = dpToPx(22f)
    private var animation: ValueAnimator? = null

    fun doOnBind(view: View?, item: MessageListItem) {
        view ?: return
        val isMultiSelectableMode = (viewHolder.bindingAdapter as? MessagesAdapter)?.isMultiSelectableMode()
                ?: false

        val message = (item as? MessageListItem.MessageItem)?.message ?: return
        if (isMultiSelectableMode && !message.isPending()) {
            view.isVisible = true
            val isSelected = message.isSelected

            if (view is CheckBox) {
                view.isChecked = isSelected
            } else view.isSelected = isSelected

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
        if (message.isPending()) {
            view.isVisible = false
            return
        }
        val isSelected = message.isSelected
        if (view is CheckBox) {
            view.isChecked = isSelected
        } else view.isSelected = isSelected

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

    fun cancelSelectableState(view: View?, item: MessageListItem) {
        view ?: return
        val message = (item as? MessageListItem.MessageItem)?.message ?: return
        if (message.isPending()) {
            view.isVisible = false
            return
        }
        view.isVisible = true
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
        })
        animation?.start()
    }
}