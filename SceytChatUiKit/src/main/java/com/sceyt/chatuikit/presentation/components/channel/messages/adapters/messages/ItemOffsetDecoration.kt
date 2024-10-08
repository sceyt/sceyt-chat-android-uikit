package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle

class ItemOffsetDecoration(private val style: MessagesListViewStyle) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        try {
            val viewHolder = parent.getChildViewHolder(view)
            val bindPos = viewHolder.bindingAdapterPosition
            val needOffset = (viewHolder.bindingAdapter as? MessagesAdapter)?.needTopOffset(bindPos)
                    ?: false
            if (needOffset)
                outRect[0, style.differentSenderMessageDistance, 0] = 0
            else outRect[0, style.sameSenderMessageDistance, 0] = 0
        } catch (ignored: Exception) {
        }
    }
}