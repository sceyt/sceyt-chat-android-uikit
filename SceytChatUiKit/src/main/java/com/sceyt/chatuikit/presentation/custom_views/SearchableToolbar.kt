package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytLayoutSearchableToolbarBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.hideKeyboard
import com.sceyt.chatuikit.extensions.showSoftInput
import com.sceyt.chatuikit.presentation.helpers.DebounceHelper
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_STYLE
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle

class SearchableToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: SceytLayoutSearchableToolbarBinding
    private var isSearchMode: Boolean = false
    private val debounceHelper by lazy { DebounceHelper(300, this) }
    private var toolbarTitle: String? = null
    private var enableSearch = true

    init {
        binding = SceytLayoutSearchableToolbarBinding.inflate(LayoutInflater.from(context), this)

        @ColorInt
        var titleColor: Int = context.getCompatColor(R.color.sceyt_color_text_primary)
        var titleTextSize = context.resources.getDimensionPixelSize(R.dimen.bigTextSize)

        context.obtainStyledAttributes(attrs, R.styleable.SearchableToolbar).use { array ->
            toolbarTitle =
                array.getString(R.styleable.SearchableToolbar_sceytUiSearchableToolbarTitle)
            titleColor = array.getColor(
                R.styleable.SearchableToolbar_sceytUiSearchableToolbarTitleColor,
                titleColor
            )
            titleTextSize = array.getDimensionPixelSize(
                R.styleable.SearchableToolbar_sceytUiSearchableToolbarTitleTextSize,
                titleTextSize
            )
            enableSearch = array.getBoolean(
                R.styleable.SearchableToolbar_sceytUiSearchableToolbarEnableSearch,
                enableSearch
            )

            val backgroundColor = array.getColor(
                R.styleable.SearchableToolbar_sceytUiSearchableToolbarBackgroundColor,
                UNSET_COLOR
            )
            val underlineColor = array.getColor(
                R.styleable.SearchableToolbar_sceytUiSearchableToolbarUnderlineColor,
                UNSET_COLOR
            )
            val navigationIcon =
                array.getDrawable(R.styleable.SearchableToolbar_sceytUiSearchableToolbarNavigationIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back)
            val iconsTint =
                array.getColor(R.styleable.SearchableToolbar_sceytUiSearchableToolbarIconsTint, 0)

            val titleFont = array.getResourceId(
                R.styleable.SearchableToolbar_sceytUiSearchableToolbarTitleTextFontFamily,
                R.font.roboto_medium
            )
            val titleStyle = array.getInt(
                R.styleable.SearchableToolbar_sceytUiSearchableToolbarTitleTextStyle,
                UNSET_STYLE
            )

            val titleTextStyle = TextStyle(
                color = titleColor,
                size = titleTextSize,
                font = titleFont,
                style = titleStyle
            )

            val inputTextStyle = TextStyle.Builder(array)
                .setColor(
                    R.styleable.SearchableToolbar_sceytUiSearchableToolbarInputTextColor,
                    context.getCompatColor(R.color.sceyt_color_text_primary)
                )
                .setSize(R.styleable.SearchableToolbar_sceytUiSearchableToolbarInputTextSize)
                .setFont(R.styleable.SearchableToolbar_sceytUiSearchableToolbarInputTextFontFamily)
                .setStyle(R.styleable.SearchableToolbar_sceytUiSearchableToolbarInputTextStyle)
                .build()

            val hintStyle = HintStyle.Builder(array)
                .hint(
                    index = R.styleable.SearchableToolbar_sceytUiSearchableToolbarSearchHintText,
                    defValue = context.getString(R.string.sceyt_search)
                )
                .color(
                    index = R.styleable.SearchableToolbar_sceytUiSearchableToolbarSearchHintColor,
                    defValue = context.getCompatColor(R.color.sceyt_color_text_footnote)
                )
                .build()

            val textInputStyle = TextInputStyle(
                textStyle = inputTextStyle,
                hintStyle = hintStyle
            )
            val searchInputStyle = SearchInputStyle.Builder(array)
                .searchIcon(
                    index = R.styleable.SearchableToolbar_sceytUiSearchableToolbarSearchIcon,
                    defValue = context.getCompatDrawable(R.drawable.sceyt_ic_search)
                )
                .clearIcon(
                    index = R.styleable.SearchableToolbar_sceytUiSearchableToolbarClearIcon,
                    defValue = context.getCompatDrawable(R.drawable.sceyt_ic_cancel)
                )
                .textInputStyle(textInputStyle)
                .build()

            val searchToolbarStyle = SearchToolbarStyle(
                toolbarStyle = ToolbarStyle(
                    backgroundColor = backgroundColor,
                    underlineColor = underlineColor,
                    navigationIcon = navigationIcon,
                    titleTextStyle = titleTextStyle
                ),
                searchInputStyle = searchInputStyle
            )

            searchToolbarStyle.apply(this@SearchableToolbar)
            applyIconsTint(iconsTint)
            binding.initViews()
        }
    }

    private fun applyIconsTint(@ColorInt tint: Int) {
        if (tint != 0) {
            binding.icSearch.setColorFilter(tint)
            binding.icBack.setColorFilter(tint)
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

    fun setTitleTextStyle(style: TextStyle) {
        style.apply(binding.tvTitle)
    }

    fun setSearchInputStyle(style: SearchInputStyle) {
        style.apply(binding.input, null, binding.icSearch, binding.icClear)
    }

    fun setBorderColor(@ColorInt color: Int) {
        binding.underline.setBackgroundColor(color)
    }

    fun setNavigationIcon(icon: Drawable?) {
        binding.icBack.setImageDrawable(icon)
    }

    fun setQueryChangeListener(listener: (String) -> Unit) {
        binding.input.addTextChangedListener {
            debounceHelper.submit {
                listener.invoke(it.toString())
            }
        }
    }

    fun setIconsTint(@ColorRes colorId: Int) {
        applyIconsTint(context.getCompatColor(colorId))
    }

    fun setNavigationClickListener(listener: OnClickListener) {
        binding.icBack.setOnClickListener(listener)
    }

    fun isSearchMode() = isSearchMode

    fun cancelSearchMode() {
        binding.serSearchMode(false)
    }
}