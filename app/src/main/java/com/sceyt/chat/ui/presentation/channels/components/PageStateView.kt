package com.sceyt.chat.ui.presentation.channels.components

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.LayoutRes

class PageStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {


    @LayoutRes
    private val loadingStateViewId: Int? = null

    @LayoutRes
    private val emptyStateViewId: Int? = null

    @LayoutRes
    private val emptySearchStateViewId: Int? = null

    init {

    }


    fun updateState(state: PageState) {
        when {
            state.isEmpty && emptyStateViewId != null -> {

            }
        }
    }
}