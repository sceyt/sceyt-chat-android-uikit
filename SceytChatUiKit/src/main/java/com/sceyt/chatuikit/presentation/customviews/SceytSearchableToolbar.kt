package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytLayoutSearchableToolbarBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.hideKeyboard
import com.sceyt.chatuikit.extensions.showSoftInput
import com.sceyt.chatuikit.presentation.uicomponents.searchinput.DebounceHelper

class SceytSearchableToolbar @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: SceytLayoutSearchableToolbarBinding
    private var isSearchMode: Boolean = false
    private val debounceHelper by lazy { DebounceHelper(300, this) }
    private var toolbarTitle: String? = null
    private var titleColor: Int
    private var enableSearch = true
    private var searchIcon: Int
    private var backIcon: Int
    private var clearIcon: Int

    @ColorInt
    private var iconsTint: Int = 0
    private var titleTextSize: Int

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SceytSearchableToolbar)
        toolbarTitle = a.getString(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarTitle)
        titleColor = a.getColor(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarTitleColor, context.getCompatColor(R.color.sceyt_color_text_primary))
        titleTextSize = a.getDimensionPixelSize(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarTitleTextSize,
            context.resources.getDimension(R.dimen.bigTextSize).toInt())
        enableSearch = a.getBoolean(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarEnableSearch, enableSearch)
        searchIcon = a.getResourceId(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarSearchIcon, R.drawable.sceyt_ic_search)
        backIcon = a.getResourceId(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarBackIcon, R.drawable.sceyt_ic_arrow_back)
        clearIcon = a.getResourceId(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarClearIcon, R.drawable.sceyt_ic_cancel)
        iconsTint = a.getColor(R.styleable.SceytSearchableToolbar_sceytSearchableToolbarIconsTint, iconsTint)
        a.recycle()

        binding = SceytLayoutSearchableToolbarBinding.inflate(LayoutInflater.from(context), this)
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
            binding.icSearch.setColorFilter(iconsTint)
            binding.icBack.setColorFilter(iconsTint)
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
                input.text = null
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
            input.text = null
            context.hideKeyboard(input)
        }
    }

    fun getQuery() = binding.input.text.toString()

    fun setTitle(title: String) {
        toolbarTitle = title.trim()
        binding.tvTitle.text = title.trim()
    }

    fun setTitleColorRes(@ColorRes colorId: Int) {
        titleColor = context.getCompatColor(colorId)
        binding.tvTitle.setTextColor(titleColor)
        binding.input.setTextColor(titleColor)
    }

    fun setQueryChangeListener(listener: (String) -> Unit) {
        binding.input.addTextChangedListener {
            debounceHelper.submit {
                listener.invoke(it.toString())
            }
        }
    }

    fun setIconsTint(@ColorRes colorId: Int) {
        iconsTint = context.getCompatColor(colorId)
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