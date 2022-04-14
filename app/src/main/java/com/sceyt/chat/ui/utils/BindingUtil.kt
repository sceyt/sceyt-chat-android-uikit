package com.sceyt.chat.ui.utils

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.Observable
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.UserPresenceStatus
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.SceytUiChannel
import com.sceyt.chat.ui.data.models.SceytUiDirectChannel
import com.sceyt.chat.ui.extencions.getCompatColorByTheme
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig

object BindingUtil {
    private val themeTextColorsViews: HashSet<Pair<View, Int>> = HashSet()
    private val backgroundColorsViews: HashSet<Pair<View, Int>> = HashSet()

    init {
        SceytUIKitConfig.SceytUITheme.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val isDark = SceytUIKitConfig.isDarkMode
                themeTextColorsViews.forEach {
                    it.first.let { view ->
                        val color = view.context.getCompatColorByTheme(it.second, isDark)
                        when (view) {
                            is TextView -> view.setTextColor(color)
                            is Toolbar -> view.setTitleTextColor(color)
                            is SwitchCompat -> view.setTextColor(color)
                        }
                    }
                }
                backgroundColorsViews.forEach {
                    it.first.let { view ->
                        view.background = ColorDrawable(view.context.getCompatColorByTheme(it.second, isDark))
                    }
                }
            }
        })
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
        view.isVisible = (channel?.channelType == ChannelTypeEnum.Direc)
                && (channel as? SceytUiDirectChannel)?.peer?.presenceStatus == UserPresenceStatus.Online
    }

    @BindingAdapter("bind:themedTextColor")
    @JvmStatic
    fun themedTextColor(view: View, color: Int) {
        val pair = Pair(view, color)

        view.doOnDetach {
            themeTextColorsViews.remove(pair)
        }

        view.doOnAttach {
            themeTextColorsViews.add(pair)
        }
    }

    /*  @BindingAdapter("bind:themedBackgroundColor")
      @JvmStatic
      fun themedBackgroundColor(view: View, color: Int) {
          val pair = Pair(view, color)

          view.doOnDetach {
              themedBackgroundColor().remove(pair)
          }

          view.doOnAttach {
              themeTextColorsViews.add(pair)
          }
      }*/

    @BindingAdapter("bind:themedBackgroundColor")
    @JvmStatic
    fun themedBackgroundColor(view: ViewGroup, color: Int) {
        val pair = Pair(view, color)

        view.doOnDetach {
            backgroundColorsViews.remove(pair)
        }

        view.doOnAttach {
            backgroundColorsViews.add(pair)
        }
    }

    private fun addOnPropertyLister() {

    }
}