package com.sceyt.sceytchatuikit.presentation.customviews

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytCustomToolbarBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class SceytCustomToolbar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: SceytCustomToolbarBinding
    private var navigationIconId = R.drawable.sceyt_ic_arrow_back
    private var menuIconId: Int = 0
    private var title = ""
    private var iconsTint = context.getCompatColor(SceytKitConfig.sceytColorAccent)
    private var enableDivider = true

    init {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SceytCustomToolbar)
            navigationIconId = typedArray.getResourceId(R.styleable.SceytCustomToolbar_navigationIcon, navigationIconId)
            menuIconId = typedArray.getResourceId(R.styleable.SceytCustomToolbar_menuIcon, menuIconId)
            title = typedArray.getString(R.styleable.SceytCustomToolbar_title) ?: title
            iconsTint = typedArray.getColor(R.styleable.SceytCustomToolbar_iconsTint, iconsTint)
            enableDivider = typedArray.getBoolean(R.styleable.SceytCustomToolbar_enableDivider, enableDivider)
            typedArray.recycle()
        }

        setupViews()
    }

    private fun setupViews() {
        binding = SceytCustomToolbarBinding.inflate(LayoutInflater.from(context), this, true)
        with(binding) {
            tvTitle.text = title
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