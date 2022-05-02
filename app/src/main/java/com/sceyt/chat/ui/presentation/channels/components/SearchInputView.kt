package com.sceyt.chat.ui.presentation.channels.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.sceyt.chat.ui.databinding.SearchViewBinding
import com.sceyt.chat.ui.extencions.debounce


class SearchInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var mBinding: SearchViewBinding

    init {
        isSaveFromParentEnabled = false
        mBinding = SearchViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private fun init() {

    }

    fun setDebouncedTextChangeListener(listener: (CharSequence?) -> Unit) {
        mBinding.searchView.debounce(cb = listener)
    }
}