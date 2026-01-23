package com.sceyt.chatuikit.presentation.common.recyclerview

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MaxHeightLinearLayoutManager(
    context: Context,
    private val maxHeight: Int
) : LinearLayoutManager(context) {

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int
    ) {
        var height = View.MeasureSpec.getSize(heightSpec)
        if (height > maxHeight) {
            height = maxHeight
        }
        super.onMeasure(
            recycler,
            state,
            widthSpec,
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
    }
}