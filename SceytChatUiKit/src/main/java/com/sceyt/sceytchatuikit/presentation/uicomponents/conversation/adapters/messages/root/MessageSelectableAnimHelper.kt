package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.ViewGroup.MarginLayoutParams
import android.widget.CheckBox
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessagesAdapter

class MessageSelectableAnimHelper(private var viewHolder: RecyclerView.ViewHolder) {
    private val checkBoxSize = dpToPx(22f)
    private var animation: ValueAnimator? = null

    fun doOnBind(checkBox: CheckBox?, message: MessageListItem) {
        checkBox ?: return
        val isMultiSelectableMode = (viewHolder.bindingAdapter as? MessagesAdapter)?.isMultiSelectableMode()
                ?: false
        checkBox.isVisible = isMultiSelectableMode

        if (isMultiSelectableMode) {
            checkBox.isChecked = (message as? MessageListItem.MessageItem)?.message?.isSelected ?: false
            checkBox.updateLayoutParams<MarginLayoutParams> {
                marginStart = checkBoxSize / 2
            }
        } else checkBox.isVisible = false
    }

    fun doOnAttach(checkBox: CheckBox?, message: MessageListItem) {
        doOnBind(checkBox, message)
    }

    fun setSelectableState(checkBox: CheckBox?, message: MessageListItem) {
        checkBox ?: return
        checkBox.isChecked = (message as? MessageListItem.MessageItem)?.message?.isSelected ?: false
        if (!checkBox.isVisible)
            checkBox.updateLayoutParams<MarginLayoutParams> {
                marginStart = -checkBoxSize
            }

        checkBox.isVisible = true
        animation?.cancel()
        animation = ObjectAnimator.ofFloat(checkBox.marginStart.toFloat(), checkBoxSize / 2f)
        animation?.addUpdateListener {
            checkBox.updateLayoutParams<MarginLayoutParams> {
                marginStart = (it.animatedValue as Float).toInt()
            }
        }
        animation?.start()
    }

    fun cancelSelectableState(checkBox: CheckBox?) {
        checkBox ?: return
        checkBox.isVisible = true
        animation?.cancel()
        animation = ObjectAnimator.ofFloat(checkBox.marginStart.toFloat(), -checkBoxSize.toFloat())
        animation?.addUpdateListener {
            checkBox.updateLayoutParams<MarginLayoutParams> {
                marginStart = (it.animatedValue as Float).toInt()
            }
        }
        animation?.addListener(onEnd = {
            if (checkBox.marginStart == -checkBoxSize)
                checkBox.isVisible = false
        })
        animation?.start()
    }
}