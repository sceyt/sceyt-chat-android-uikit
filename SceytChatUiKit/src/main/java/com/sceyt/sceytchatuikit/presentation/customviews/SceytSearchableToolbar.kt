package com.sceyt.sceytchatuikit.presentation.customviews

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytLayoutSearchableToolbarBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.hideKeyboard
import com.sceyt.sceytchatuikit.extensions.showSoftInput
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper

class SceytSearchableToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private val binding: SceytLayoutSearchableToolbarBinding
    private var isSearchMode: Boolean = false
    private val debounceHelper by lazy { DebounceHelper(300, this) }
    private var toolbarTitle: String? = null
    private var titleColor: Int
    private var enableSearch = true
    private var searchIcon: Int
    private var backIcon: Int
    private var clearIcon: Int
    private var iconsTint: Int = 0
    private var titleTextSize: Int

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SceytSearchableToolbar)
        toolbarTitle = a.getString(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarTitle)
        titleColor = a.getColor(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarTitleColor, context.getCompatColor(R.color.sceyt_color_text_themed))
        titleTextSize = a.getDimensionPixelSize(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarTitleTextSize,
            context.resources.getDimension(R.dimen.bigTextSize).toInt())
        enableSearch = a.getBoolean(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarEnableSearch, enableSearch)
        searchIcon = a.getResourceId(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarSearchIcon, R.drawable.sceyt_ic_search)
        backIcon = a.getResourceId(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarBackIcon, R.drawable.sceyt_ic_arrow_back)
        clearIcon = a.getResourceId(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarClearIcon, R.drawable.sceyt_ic_cancel)
        iconsTint = a.getResourceId(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarIconsTint, iconsTint)
        a.recycle()

        binding = SceytLayoutSearchableToolbarBinding.inflate(LayoutInflater.from(context), this, true)
        binding.initViews()
        setIconsAndColors()
    }

    private fun setIconsAndColors() {
        binding.icSearch.setImageResource(searchIcon)
        binding.icBack.setImageResource(backIcon)
        binding.tvTitle.apply {
            setTextColor(titleColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, titleTextSize.toFloat())
        }
        if (iconsTint != 0) {
            binding.icSearch.setColorFilter(context.getCompatColor(iconsTint))
            binding.icBack.setColorFilter(context.getCompatColor(iconsTint))
        }
    }

    private fun SceytLayoutSearchableToolbarBinding.initViews() {
        tvTitle.text = toolbarTitle
        icSearch.isVisible = enableSearch

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

    fun setTitle(title: String) {
        toolbarTitle = title.trim()
        binding.tvTitle.text = title.trim()
    }

    fun setQueryChangeListener(listener: (String) -> Unit) {
        binding.input.addTextChangedListener {
            debounceHelper.submit {
                listener.invoke(it.toString())
            }
        }
    }

    fun setIconsTint(@ColorRes colorId: Int) {
        iconsTint = colorId
        setIconsAndColors()
    }

    fun setNavigationIconClickListener(listener: OnClickListener) {
        binding.icBack.setOnClickListener(listener)
    }

    fun isSearchMode() = isSearchMode

    fun cancelSearchMode() {
        binding.serSearchMode(false)
    }
}