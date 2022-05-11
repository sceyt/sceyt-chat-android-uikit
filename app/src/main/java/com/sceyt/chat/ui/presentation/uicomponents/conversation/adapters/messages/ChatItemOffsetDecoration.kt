package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView

class ChatItemOffsetDecoration(private val mItemOffset: Int) : RecyclerView.ItemDecoration() {
    constructor(context: Context, @DimenRes itemOffsetId: Int) : this(context.resources.getDimensionPixelSize(itemOffsetId))

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        try {
            val viewHolder = parent.getChildViewHolder(view)
            val bindPos = viewHolder.bindingAdapterPosition
            val needOffset = (viewHolder.bindingAdapter as? MessagesAdapter)?.needTopOffset(bindPos)
                    ?: false
            if (needOffset)
                outRect[0, mItemOffset, 0] = 0
        } catch (ignored: Exception) {
        }
    }
}