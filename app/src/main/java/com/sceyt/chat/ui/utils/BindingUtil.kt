package com.sceyt.chat.ui.utils

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.Observable
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.ui.BR
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.SceytUiChannel
import com.sceyt.chat.ui.data.models.SceytUiDirectChannel
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig

object BindingUtil {
    private val themeTextColorsViews: HashSet<Pair<View, Int>> = HashSet()
    private val backgroundColorsViews: HashSet<Pair<View, Int>> = HashSet()

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

    private fun setThemedBackground(view: View, color: Int, isDark: Boolean) {
        view.background = ColorDrawable(view.context.getCompatColorByTheme(color, isDark))
    }

    @BindingAdapter("bind:status", "bind:incoming")
    @JvmStatic
    fun setMessageStatusIcon(imageView: ImageView, status: DeliveryStatus?, incoming: Boolean?) {
        if (status == null || incoming == true) {
            imageView.isVisible = false
            return
        }
        val iconResId = when (status) {
            DeliveryStatus.Pending -> R.drawable.ic_status_not_sent
            DeliveryStatus.Sent -> R.drawable.ic_status_on_server
            DeliveryStatus.Delivered -> R.drawable.ic_status_delivered
            DeliveryStatus.Read -> R.drawable.ic_status_read
            else -> null
        }
        iconResId?.let {
            imageView.setImageResource(it)
            imageView.isVisible = true
        }
    }

    @BindingAdapter("bind:visibleIf")
    @JvmStatic
    fun visibleIf(anyView: View, show: Boolean) {
        anyView.visibility = if (show) View.VISIBLE else View.GONE
    }

    @BindingAdapter("bind:channel")
    @JvmStatic
    fun setOnlineStatus(view: View, channel: SceytUiChannel?) {
        view.isVisible = (channel?.channelType == ChannelTypeEnum.Direct)
                && (channel as? SceytUiDirectChannel)?.peer?.presence?.state == PresenceState.Online
    }

    @BindingAdapter("bind:themedTextColor")
    @JvmStatic
    fun themedTextColor(view: View, @ColorRes colorId: Int) {
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

    @BindingAdapter("bind:themedBackgroundColor")
    @JvmStatic
    fun themedBackgroundColor(view: ViewGroup, @ColorRes colorId: Int) {
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
}