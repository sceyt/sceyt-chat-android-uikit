package com.sceyt.chat.ui.utils

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
import com.sceyt.chat.ui.BR
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.extensions.getCompatDrawableByTheme
import com.sceyt.chat.ui.presentation.customviews.SceytOnlineView
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig

object BindingUtil {
    private val themeTextColorsViews: HashSet<Pair<View, Int>> = HashSet()
    private val backgroundColorsViews: HashSet<Pair<View, Int>> = HashSet()
    private val backgroundTintColorsViews: HashSet<Pair<View, Int>> = HashSet()
    private val themeDrawablesViews: HashSet<Pair<ImageView, Int>> = HashSet()
    private val themeStrokeColorOnlineViews: HashSet<Pair<SceytOnlineView, Int>> = HashSet()

    init {
        SceytUIKitConfig.SceytUITheme.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if (propertyId == BR.isDarkMode) {
                    val isDark = SceytUIKitConfig.isDarkMode
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
        if (colorId == null) return
        val pair = Pair(view, colorId)
        themeTextColorsViews.add(pair)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                setThemedColor(view, colorId, SceytUIKitConfig.isDarkMode)
                themeTextColorsViews.add(pair)
            }

            override fun onViewDetachedFromWindow(p0: View) {
                themeTextColorsViews.remove(pair)
            }
        })
    }

    @BindingAdapter("themedBackgroundColor")
    @JvmStatic
    fun themedBackgroundColor(view: View, @ColorRes colorId: Int?) {
        colorId ?: return
        setThemedBackground(view, colorId, SceytUIKitConfig.isDarkMode)
        val pair = Pair(view, colorId)
        backgroundColorsViews.add(pair)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                setThemedBackground(view, colorId, SceytUIKitConfig.isDarkMode)
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
        setThemedBackgroundTint(view, colorId, SceytUIKitConfig.isDarkMode)
        val pair = Pair(view, colorId)
        backgroundTintColorsViews.add(pair)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                setThemedBackgroundTint(view, colorId, SceytUIKitConfig.isDarkMode)
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
        drawableId ?: return
        setThemedDrawable(view, drawableId, SceytUIKitConfig.isDarkMode)
        val pair = Pair(view, drawableId)
        themeDrawablesViews.add(pair)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                setThemedDrawable(view, drawableId, SceytUIKitConfig.isDarkMode)
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
        view.setStrokeColor(view.context.getCompatColorByTheme(colorId, SceytUIKitConfig.isDarkMode))
        val pair = Pair(view, colorId)
        themeStrokeColorOnlineViews.add(pair)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                view.setStrokeColor(view.context.getCompatColorByTheme(colorId, SceytUIKitConfig.isDarkMode))
                themeStrokeColorOnlineViews.add(pair)
            }

            override fun onViewDetachedFromWindow(p0: View) {
                themeStrokeColorOnlineViews.remove(pair)
            }
        })
    }
}