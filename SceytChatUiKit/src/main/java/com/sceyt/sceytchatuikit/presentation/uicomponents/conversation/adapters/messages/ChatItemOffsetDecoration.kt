package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ChatItemOffsetDecoration(private val mItemOffset: Int) : RecyclerView.ItemDecoration() {

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