package com.sceyt.sceytchatuikit.shared.utils

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import androidx.databinding.Observable
import com.sceyt.sceytchatuikit.BR
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.extensions.getCompatDrawableByTheme
import com.sceyt.sceytchatuikit.presentation.customviews.SceytColorSpannableTextView
import com.sceyt.sceytchatuikit.presentation.customviews.SceytOnlineView
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

object BindingUtil {
    private val themeTextColorsViews: HashSet<Pair<View, Int>> = HashSet()
    private val themeColorSpannableTextViews: HashSet<SceytColorSpannableTextView> = HashSet()
    private val backgroundColorsViews: HashSet<Pair<View, Int>> = HashSet()
    private val backgroundTintColorsViews: HashSet<Pair<View, Int>> = HashSet()
    private val themeDrawablesViews: HashSet<Pair<ImageView, Int>> = HashSet()
    private val themeStrokeColorOnlineViews: HashSet<Pair<SceytOnlineView, Int>> = HashSet()

    init {
        if (SceytKitConfig.enableDarkMode)
            SceytKitConfig.SceytUITheme.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    if (propertyId == BR.isDarkMode) {
                        val isDark = SceytKitConfig.isDarkMode
                        themeTextColorsViews.forEach {
                            setThemedColor(it.first, it.second, isDark)
                        }
                        backgroundColorsViews.forEach {
                            setThemedBackground(it.first, it.second, isDark)
                        }
                        backgroundTintColorsViews.forEach {
                            setThemedBackgroundTint(it.first, it.second, isDark)
                        }
                        themeDrawablesViews.forEach {
                            setThemedDrawable(it.first, it.second, isDark)
                        }
                        themeStrokeColorOnlineViews.forEach {
                            val view = it.first
                            view.setStrokeColor(view.context.getCompatColorByTheme(it.second, isDark))
                        }
                        themeColorSpannableTextViews.forEach {
                            it.invalidateColor()
                        }
                    }
                }
            })
    }

    private fun setThemedColor(view: View, colorId: Int, isDark: Boolean) {
        val color = view.context.getCompatColorByTheme(colorId, isDark)
        when (view) {
            is TextView -> view.setTextColor(color)
            is Toolbar -> view.setTitleTextColor(color)
            is SwitchCompat -> view.setTextColor(color)
        }
    }

    private fun setThemedBackground(view: View, color: Int?, isDark: Boolean) {
        view.background = ColorDrawable(view.context.getCompatColorByTheme(color, isDark))
    }

    private fun setThemedBackgroundTint(view: View, color: Int, isDark: Boolean) {
        view.backgroundTintList = ColorStateList.valueOf(view.context.getCompatColorByTheme(color, isDark))
    }

    private fun setThemedDrawable(view: ImageView, drawableId: Int, isDark: Boolean) {
        view.setImageDrawable(view.context.getCompatDrawableByTheme(drawableId, isDark))
    }

    @BindingAdapter("visibleIf")
    @JvmStatic
    fun visibleIf(anyView: View, show: Boolean) {
        anyView.visibility = if (show) View.VISIBLE else View.GONE
    }

    @BindingAdapter("themedTextColor")
    @JvmStatic
    fun themedTextColor(view: View, colorId: Int?) {
        if (colorId == null || !SceytKitConfig.enableDarkMode) return
        val pair = Pair(view, colorId)
        themeTextColorsViews.add(pair)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                setThemedColor(view, colorId, SceytKitConfig.isDarkMode)
                themeTextColorsViews.add(pair)
            }

            override fun onViewDetachedFromWindow(p0: View) {
                themeTextColorsViews.remove(pair)
            }
        })
    }

    @BindingAdapter("themedSpannableTextColor")
    @JvmStatic
    fun themedSpannableTextColor(view: SceytColorSpannableTextView, param: Boolean) {
        if (!SceytKitConfig.enableDarkMode) return
        themeColorSpannableTextViews.add(view)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                view.invalidateColor()
                themeColorSpannableTextViews.add(view)
            }

            override fun onViewDetachedFromWindow(p0: View) {
                themeColorSpannableTextViews.remove(view)
            }
        })
    }

    @BindingAdapter("themedBackgroundColor")
    @JvmStatic
    fun themedBackgroundColor(view: View, @ColorRes colorId: Int?) {
        if (colorId == null || !SceytKitConfig.enableDarkMode) return
        setThemedBackground(view, colorId, SceytKitConfig.isDarkMode)
        val pair = Pair(view, colorId)
        backgroundColorsViews.add(pair)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                setThemedBackground(view, colorId, SceytKitConfig.isDarkMode)
                backgroundColorsViews.add(pair)
            }

            override fun onViewDetachedFromWindow(p0: View) {
                backgroundColorsViews.remove(pair)
            }
        })
    }

    @BindingAdapter("themedBackgroundTintColor")
    @JvmStatic
    fun themedBackgroundTintColor(view: View, @ColorRes colorId: Int) {
        if (!SceytKitConfig.enableDarkMode) return
        setThemedBackgroundTint(view, colorId, SceytKitConfig.isDarkMode)
        val pair = Pair(view, colorId)
        backgroundTintColorsViews.add(pair)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                setThemedBackgroundTint(view, colorId, SceytKitConfig.isDarkMode)
                backgroundTintColorsViews.add(pair)
            }

            override fun onViewDetachedFromWindow(p0: View) {
                backgroundTintColorsViews.remove(pair)
            }
        })
    }

    @BindingAdapter("themedDrawable")
    @JvmStatic
    fun themedDrawable(view: ImageView, @DrawableRes drawableId: Int?) {
        if (drawableId == null || !SceytKitConfig.enableDarkMode) return
        setThemedDrawable(view, drawableId, SceytKitConfig.isDarkMode)
        val pair = Pair(view, drawableId)
        themeDrawablesViews.add(pair)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                setThemedDrawable(view, drawableId, SceytKitConfig.isDarkMode)
                themeDrawablesViews.add(pair)
            }

            override fun onViewDetachedFromWindow(p0: View) {
                themeDrawablesViews.remove(pair)
            }
        })
    }

    @BindingAdapter("themeStrokeColorOnlineView")
    @JvmStatic
    fun themeStrokeColorOnlineView(view: SceytOnlineView, @ColorRes colorId: Int) {
        if (!SceytKitConfig.enableDarkMode) return
        view.setStrokeColor(view.context.getCompatColorByTheme(colorId, SceytKitConfig.isDarkMode))
        val pair = Pair(view, colorId)
        themeStrokeColorOnlineViews.add(pair)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                view.setStrokeColor(view.context.getCompatColorByTheme(colorId, SceytKitConfig.isDarkMode))
                themeStrokeColorOnlineViews.add(pair)
            }

            override fun onViewDetachedFromWindow(p0: View) {
                themeStrokeColorOnlineViews.remove(pair)
            }
        })
    }
}