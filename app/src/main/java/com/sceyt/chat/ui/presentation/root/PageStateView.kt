package com.sceyt.chat.ui.presentation.root

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.sceyt.chat.ui.extensions.customToastSnackBar

class PageStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val layoutInflater by lazy { LayoutInflater.from(context) }

    private var loadingStateView: View? = null
    private var emptyStateView: View? = null
    private var emptySearchStateView: View? = null


    fun setLoadingStateView(@LayoutRes id: Int) {
        val view = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, id, this, false)
        addView(view.root.apply {
            isVisible = false
            loadingStateView = this
        })
    }

    fun setEmptyStateView(@LayoutRes id: Int) {
        val view = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, id, this, false)
        addView(view.root.apply {
            isVisible = false
            emptyStateView = this
        })
    }

    fun setEmptySearchStateView(@LayoutRes id: Int) {
        val view = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, id, this, false)
        addView(view.root.apply {
            isVisible = false
            emptySearchStateView = this
        })
    }

    fun updateState(state: PageState, showLoadingIfNeed: Boolean = true) {
        when (state) {
            is PageState.StateEmpty -> {
                emptyStateView?.isVisible = !state.isSearch && !state.wasLoadingMore
                emptySearchStateView?.isVisible = state.isSearch && !state.wasLoadingMore
                loadingStateView?.isVisible = false
            }
            is PageState.StateLoading -> {
                emptyStateView?.isVisible = false
                emptySearchStateView?.isVisible = false
                loadingStateView?.isVisible = state.isLoading && showLoadingIfNeed
            }
            is PageState.StateError -> {
                customToastSnackBar(this, state.errorMessage.toString())
                emptyStateView?.isVisible = false
                emptySearchStateView?.isVisible = false
                loadingStateView?.isVisible = false
            }
            is PageState.StateLoadingMore -> return
            is PageState.Nothing -> {
                emptyStateView?.isVisible = false
                emptySearchStateView?.isVisible = false
                loadingStateView?.isVisible = false
            }
        }
    }
}