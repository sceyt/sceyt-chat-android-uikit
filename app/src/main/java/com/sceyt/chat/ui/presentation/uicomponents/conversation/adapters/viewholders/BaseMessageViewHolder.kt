package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import com.sceyt.chat.ui.presentation.customviews.ToReplayLineView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.MessageListItem

abstract class BaseMessageViewHolder(view: View) : BaseViewHolder<MessageListItem>(view) {

    val INC_DEFAULT_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    val INC_EDITED_SPACE =
            "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    val OUT_DEFAULT_SPACE =
            "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    val OUT_EDITED_SPACE =
            "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"


    protected fun setReplayCountLineMargins(measuringView: View, replayCountView: View, toReplayLine: ToReplayLineView) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        measuringView.measure(widthSpec, heightSpec)
        val messageInfoHeight: Int = measuringView.measuredHeight

        replayCountView.measure(widthSpec, heightSpec)
        val tvReplayCountHeight: Int = replayCountView.measuredHeight

        (toReplayLine.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0,
            messageInfoHeight / 2, 0, tvReplayCountHeight / 2 - replayCountView.paddingTop)
        toReplayLine.requestLayout()
    }
/*
    protected fun isDayChanged(message: Message?, position: Int): Boolean {
        return try {
            val prevMessage: Message? = getItem(position - 1)
            DateTimeUtil.isSameDay()
            return !isSameDay(message?.createdAt?.time ?: 0, prevMessage?.createdAt?.time ?: 0)
        } catch (e: IndexOutOfBoundsException) {
            refreshForDayPosition = message
            true
        }
    }*/
}