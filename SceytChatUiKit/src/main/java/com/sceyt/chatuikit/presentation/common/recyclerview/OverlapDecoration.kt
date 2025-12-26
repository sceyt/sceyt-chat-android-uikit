package com.sceyt.chatuikit.presentation.common.recyclerview

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class OverlapDecoration(
    private val overlapHeight: Int,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)

        if (position != 0) { // Apply negative margin to all items except the first one
            // The negative value makes the current item overlap the one before it
            outRect.left = -overlapHeight
        }
    }
}