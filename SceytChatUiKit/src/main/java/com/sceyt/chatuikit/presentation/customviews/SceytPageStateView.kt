package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.presentation.root.PageState

class SceytPageStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val layoutInflater by lazy { LayoutInflater.from(context) }

    private var loadingStateView: View? = null
    private var emptyStateView: View? = null
    private var emptySearchStateView: View? = null

    private fun inflateView(@LayoutRes id: Int): View {
        val viewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, id, this, false)
        return viewDataBinding?.root ?: layoutInflater.inflate(id, this, false)
    }

    fun setLoadingStateView(@LayoutRes id: Int): View {
        val view = inflateView(id)
        addView(view.apply {
            isVisible = false
            loadingStateView = this
        })
        return view
    }

    fun setEmptyStateView(@LayoutRes id: Int): View {
        val view = inflateView(id)
        addView(view.apply {
            isVisible = false
            emptyStateView = this
        })
        return view
    }

    fun setEmptySearchStateView(@LayoutRes id: Int): View {
        val view = inflateView(id)
        addView(view.apply {
            isVisible = false
            emptySearchStateView = this
        })
        return view
    }

    fun updateState(state: PageState, showLoadingIfNeed: Boolean = true, enableErrorSnackBar: Boolean = true) {
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
                if (enableErrorSnackBar && state.showMessage)
                    customToastSnackBar(this, state.errorMessage.toString())
                emptyStateView?.isVisible = false
                emptySearchStateView?.isVisible = false
                loadingStateView?.isVisible = false
            }

            is PageState.StateLoadingMore -> {
                emptyStateView?.isVisible = false
                emptySearchStateView?.isVisible = false
                loadingStateView?.isVisible = false
            }

            is PageState.Nothing -> {
                emptyStateView?.isVisible = false
                emptySearchStateView?.isVisible = false
                loadingStateView?.isVisible = false
            }
        }
    }
}