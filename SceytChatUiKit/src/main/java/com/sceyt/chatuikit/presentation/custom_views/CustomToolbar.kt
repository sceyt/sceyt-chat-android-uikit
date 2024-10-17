package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.MenuRes
import androidx.core.content.res.use
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytCustomToolbarBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.setIconsTint
import com.sceyt.chatuikit.extensions.setTint
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_RESOURCE
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

class CustomToolbar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: SceytCustomToolbarBinding
    private var navigationIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back)
    private var title = ""
    private var subTitle = ""
    private var titleTextStyle: TextStyle
    private var subtitleTextStyle: TextStyle
    private var iconsTint = UNSET_COLOR
    private var enableDivider = true
    private var dividerColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
    private val divider: View
        get() = binding.divider

    @MenuRes
    private var menuRes: Int = UNSET_RESOURCE

    init {
        @ColorInt
        var titleColor: Int = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor)
        var subtitleColor: Int = context.getCompatColor(SceytChatUIKitTheme.colors.textSecondaryColor)
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.CustomToolbar).use { typedArray ->
                navigationIcon = typedArray.getDrawable(R.styleable.CustomToolbar_sceytUiToolbarNavigationIcon)
                        ?: navigationIcon
                title = typedArray.getString(R.styleable.CustomToolbar_sceytUiToolbarTitle) ?: title
                subTitle = typedArray.getString(R.styleable.CustomToolbar_sceytUiToolbarSubtitle)
                        ?: subTitle
                iconsTint = typedArray.getColor(R.styleable.CustomToolbar_sceytUiToolbarIconsTint, iconsTint)
                titleColor = typedArray.getColor(R.styleable.CustomToolbar_sceytUiToolbarTitleTextColor, titleColor)
                subtitleColor = typedArray.getColor(R.styleable.CustomToolbar_sceytUiToolbarSubtitleTextColor, subtitleColor)
                enableDivider = typedArray.getBoolean(R.styleable.CustomToolbar_sceytUiToolbarEnableDivider, enableDivider)
                dividerColor = typedArray.getColor(R.styleable.CustomToolbar_sceytUiToolbarDividerColor, dividerColor)
                menuRes = typedArray.getResourceId(R.styleable.CustomToolbar_sceytUiToolbarMenu, UNSET_RESOURCE)
            }
        }

        titleTextStyle = TextStyle(
            color = titleColor,
            font = R.font.roboto_medium
        )

        subtitleTextStyle = TextStyle(color = subtitleColor)
        setupViews()
    }

    private fun setupViews() {
        binding = SceytCustomToolbarBinding.inflate(LayoutInflater.from(context), this)
        with(binding) {
            tvTitle.apply {
                titleTextStyle.apply(this)
                text = title
            }
            tvSubtitle.apply {
                subtitleTextStyle.apply(this)
                text = subTitle
                isVisible = subTitle.isNotEmpty()
            }
            icBack.setImageDrawable(navigationIcon)
            divider.setBackgroundColor(dividerColor)

            if (iconsTint != UNSET_COLOR) {
                setIconsTintByColor(iconsTint)
            }

            if (menuRes != UNSET_RESOURCE) {
                rootToolbar.inflateMenu(menuRes)
            }
        }
    }

    fun setTitle(title: CharSequence?) {
        title ?: return
        binding.tvTitle.text = title
    }

    fun setSubtitle(title: CharSequence?) {
        title ?: return
        binding.tvSubtitle.text = title
        binding.tvSubtitle.isVisible = title.isNotEmpty()
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

    fun setSubtitleColor(@ColorInt color: Int) {
        subtitleTextStyle = subtitleTextStyle.copy(color = color)
        binding.tvSubtitle.setTextColor(color)
    }

    fun setIconsTint(@ColorRes colorId: Int) {
        setIconsTintByColor(context.getCompatColor(colorId))
    }

    fun setIconsTintByColor(@ColorInt color: Int) {
        binding.icBack.setTint(color)
        binding.rootToolbar.menu.setIconsTint(color)
    }

    fun setBorderColor(@ColorInt color: Int) {
        dividerColor = color
        divider.setBackgroundColor(color)
    }

    fun setNavigationIcon(drawable: Drawable?) {
        navigationIcon = drawable
        binding.icBack.setImageDrawable(drawable)
        binding.icBack.isVisible = drawable != null
    }

    fun setTitleTextStyle(style: TextStyle) {
        titleTextStyle = style
        titleTextStyle.apply(binding.tvTitle)
    }

    fun setSubtitleTextStyle(style: TextStyle) {
        subtitleTextStyle = style
        subtitleTextStyle.apply(binding.tvSubtitle)
    }

    fun setNavigationClickListener(listener: () -> Unit) {
        binding.icBack.setOnClickListener { listener.invoke() }
    }

    fun inflateMenu(@MenuRes menuRes: Int) {
        this.menuRes = menuRes
        binding.rootToolbar.inflateMenu(menuRes)
    }

    fun getMenu(): Menu {
        return binding.rootToolbar.menu
    }

    fun setMenuClickListener(listener: (item: Int) -> Unit) {
        binding.rootToolbar.setOnMenuItemClickListener {
            listener.invoke(it.itemId)
            true
        }
    }
}