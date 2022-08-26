package com.sceyt.sceytchatuikit.presentation.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytLayoutSearchableToolbarBinding
import com.sceyt.sceytchatuikit.extensions.hideKeyboard
import com.sceyt.sceytchatuikit.extensions.showSoftInput
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper

class SceytSearchableToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private val binding: SceytLayoutSearchableToolbarBinding
    private var isSearchMode: Boolean = false
    private val debounceHelper by lazy { DebounceHelper(300) }
    private var toolbarTitle: String? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SceytSearchableToolbar)
        toolbarTitle = a.getString(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarTitle)
        a.recycle()

        binding = SceytLayoutSearchableToolbarBinding.inflate(LayoutInflater.from(context), this, true)
        binding.initViews()
    }

    private fun SceytLayoutSearchableToolbarBinding.initViews() {
        tvTitle.text = toolbarTitle

        icSearch.setOnClickListener {
            serSearchMode(true)
        }

        icClear.setOnClickListener {
            if (input.text.isNullOrBlank())
                serSearchMode(false)
            else {
                input.setText("")
            }
        }
    }

    private fun SceytLayoutSearchableToolbarBinding.serSearchMode(searchMode: Boolean) {
        isSearchMode = searchMode
        icSearch.isVisible = !searchMode
        icClear.isVisible = searchMode
        tvTitle.isVisible = !searchMode
        input.isVisible = searchMode

        if (searchMode) {
            context.showSoftInput(input)
        } else {
            input.setText("")
            context.hideKeyboard(input)
        }
    }

    fun getQuery() = binding.input.text.toString()

    fun setQueryChangeListener(listener: (String) -> Unit) {
        binding.input.addTextChangedListener {
            debounceHelper.submit {
                listener.invoke(it.toString())
            }
        }
    }

    fun setBackClickListener(listener: OnClickListener) {
        binding.icBack.setOnClickListener(listener)
    }

    fun isSearchMode() = isSearchMode

    fun cancelSearchMode() {
        binding.serSearchMode(false)
    }
}