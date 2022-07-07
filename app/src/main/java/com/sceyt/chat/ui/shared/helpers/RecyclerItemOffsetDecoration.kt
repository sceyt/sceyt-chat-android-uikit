package com.sceyt.chat.ui.shared.helpers

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView

class RecyclerItemOffsetDecoration(private val left: Int = 0,
                                   private val top: Int = 0,
                                   private val right: Int = 0,
                                   private var bottom: Int = 0) : RecyclerView.ItemDecoration() {
    constructor(offset: Int) : this(offset, offset, offset, offset)

    constructor(context: Context, @DimenRes itemOffsetId: Int) : this(context.resources.getDimensionPixelSize(itemOffsetId))

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        outRect.set(left, top, right, bottom)
    }
}