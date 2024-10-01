package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytCustomToolbarBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.common.TextStyle

class CustomToolbar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: SceytCustomToolbarBinding
    private var navigationIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back)
    private var menuIconId: Int = 0
    private var title = ""
    private var titleTextStyle: TextStyle
    private var iconsTint = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    private var enableDivider = true

    init {
        @ColorInt
        var titleColor: Int = context.getCompatColor(R.color.sceyt_color_text_primary)
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.CustomToolbar).use { typedArray ->
                navigationIcon = typedArray.getDrawable(R.styleable.CustomToolbar_sceytUiToolbarNavigationIcon)
                        ?: navigationIcon
                menuIconId = typedArray.getResourceId(R.styleable.CustomToolbar_sceytUiToolbarMenuIcon, menuIconId)
                title = typedArray.getString(R.styleable.CustomToolbar_sceytUiToolbarTitle) ?: title
                iconsTint = typedArray.getColor(R.styleable.CustomToolbar_sceytUiToolbarIconsTint, iconsTint)
                titleColor = typedArray.getColor(R.styleable.CustomToolbar_sceytUiToolbarTitleTextColor, titleColor)
                enableDivider = typedArray.getBoolean(R.styleable.CustomToolbar_sceytUiToolbarEnableDivider, enableDivider)
            }
        }

        titleTextStyle = TextStyle(
            color = titleColor,
            font = R.font.roboto_medium
        )

        setupViews()
    }

    private fun setupViews() {
        binding = SceytCustomToolbarBinding.inflate(LayoutInflater.from(context), this)
        with(binding) {
            tvTitle.apply {
                titleTextStyle.apply(this)
                text = title
            }
            icBack.setImageDrawable(navigationIcon)
            underline.isVisible = enableDivider
        }
        setIconsTintByColor(iconsTint)
        if (menuIconId != 0)
            setMenuIcon(menuIconId)
    }

    private fun setMenuIcon(resId: Int) {
        binding.icMenuIcon.setImageResource(resId)
        binding.icMenuIcon.isVisible = true
    }

    fun initMenuIconWithClickListener(resId: Int, listener: () -> Unit) {
        setMenuIcon(resId)
        setMenuIconClickListener(listener)
    }

    fun initNavigationIconWithClickListener(resId: Int, listener: () -> Unit) {
        binding.icBack.setImageResource(resId)
        setNavigationClickListener(listener)
    }

    fun setTitle(title: String?) {
        title ?: return
        binding.tvTitle.text = title
    }

    fun setTitleColorRes(@ColorRes colorId: Int) {
        val titleColor = context.getCompatColor(colorId)
        titleTextStyle = titleTextStyle.copy(color = titleColor)
        binding.tvTitle.setTextColor(titleColor)
    }

    fun setTitleColor(@ColorInt color: Int) {
        titleTextStyle = titleTextStyle.copy(color = color)
        binding.tvTitle.setTextColor(color)
    }

    fun setIconsTint(@ColorRes colorId: Int) {
        binding.icBack.imageTintList = ColorStateList.valueOf(context.getCompatColor(colorId))
        binding.icMenuIcon.imageTintList = ColorStateList.valueOf(context.getCompatColor(colorId))
    }

    fun setIconsTintByColor(@ColorInt color: Int) {
        binding.icBack.imageTintList = ColorStateList.valueOf(color)
        binding.icMenuIcon.imageTintList = ColorStateList.valueOf(color)
    }

    fun setBorderColor(@ColorInt color: Int) {
        binding.underline.setBackgroundColor(color)
    }

    fun setNavigationIcon(drawable: Drawable?) {
        navigationIcon = drawable
        binding.icBack.setImageDrawable(drawable)
    }

    fun setTitleTextStyle(style: TextStyle) {
        titleTextStyle = style
        titleTextStyle.apply(binding.tvTitle)
    }

    fun setNavigationClickListener(listener: () -> Unit) {
        binding.icBack.setOnClickListener { listener.invoke() }
    }

    fun setMenuIconClickListener(listener: () -> Unit) {
        binding.icMenuIcon.setOnClickListener { listener.invoke() }
    }
}