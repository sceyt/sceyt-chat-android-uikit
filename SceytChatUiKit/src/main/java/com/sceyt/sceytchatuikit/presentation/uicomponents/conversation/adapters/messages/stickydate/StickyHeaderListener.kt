package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.stickydate

import androidx.recyclerview.widget.RecyclerView

interface StickyHeaderListener {
    fun needShow()
    fun move(toFloat: Float)
    fun getHeaderViewForItem(topChildPosition: Int, parent: RecyclerView): StycyDateView
}