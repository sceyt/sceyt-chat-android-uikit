package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytCustomToolbarBinding
import com.sceyt.chatuikit.extensions.getCompatColor

class CustomToolbar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: SceytCustomToolbarBinding
    private var navigationIconId = R.drawable.sceyt_ic_arrow_back
    private var menuIconId: Int = 0
    private var title = ""
    private var titleColor = context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor)
    private var iconsTint = context.getCompatColor(SceytChatUIKit.theme.accentColor)
    private var enableDivider = true

    init {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomToolbar)
            navigationIconId = typedArray.getResourceId(R.styleable.CustomToolbar_sceytUiToolbarNavigationIcon, navigationIconId)
            menuIconId = typedArray.getResourceId(R.styleable.CustomToolbar_sceytUiToolbarMenuIcon, menuIconId)
            title = typedArray.getString(R.styleable.CustomToolbar_sceytUiToolbarTitle) ?: title
            iconsTint = typedArray.getColor(R.styleable.CustomToolbar_sceytUiToolbarIconsTint, iconsTint)
            titleColor = typedArray.getColor(R.styleable.CustomToolbar_sceytUiToolbarTitleTextColor, titleColor)
            enableDivider = typedArray.getBoolean(R.styleable.CustomToolbar_sceytUiToolbarEnableDivider, enableDivider)
            typedArray.recycle()
        }

        setupViews()
    }

    private fun setupViews() {
        binding = SceytCustomToolbarBinding.inflate(LayoutInflater.from(context), this)
        with(binding) {
            tvTitle.apply {
                setTextColor(titleColor)
                text = title
            }
            icBack.setImageResource(navigationIconId)
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

    val navigationIcon get() = binding.icBack

    val menuIcon get() = binding.icMenuIcon

    fun initMenuIconWithClickListener(resId: Int, listener: () -> Unit) {
        setMenuIcon(resId)
        setMenuIconClickListener(listener)
    }

    fun initNavigationIconWithClickListener(resId: Int, listener: () -> Unit) {
        binding.icBack.setImageResource(resId)
        setNavigationIconClickListener(listener)
    }

    fun setTitle(title: String?) {
        title ?: return
        binding.tvTitle.text = title
    }

    fun setTitleColorRes(@ColorRes colorId: Int) {
        titleColor = context.getCompatColor(colorId)
        binding.tvTitle.setTextColor(titleColor)
    }

    fun setTitleColor(@ColorInt color: Int) {
        titleColor = color
        binding.tvTitle.setTextColor(titleColor)
    }

    fun setIconsTint(@ColorRes colorId: Int) {
        binding.icBack.imageTintList = ColorStateList.valueOf(context.getCompatColor(colorId))
        binding.icMenuIcon.imageTintList = ColorStateList.valueOf(context.getCompatColor(colorId))
    }

    fun setIconsTintByColor(color: Int) {
        binding.icBack.imageTintList = ColorStateList.valueOf(color)
        binding.icMenuIcon.imageTintList = ColorStateList.valueOf(color)
    }

    fun setNavigationIconClickListener(listener: () -> Unit) {
        binding.icBack.setOnClickListener { listener.invoke() }
    }

    fun setMenuIconClickListener(listener: () -> Unit) {
        binding.icMenuIcon.setOnClickListener { listener.invoke() }
    }
}