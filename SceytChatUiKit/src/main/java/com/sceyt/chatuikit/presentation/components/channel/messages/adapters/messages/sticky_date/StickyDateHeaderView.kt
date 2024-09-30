package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.sticky_date

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.sceyt.chatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.chatuikit.extensions.changeAlphaWithValueAnim
import com.sceyt.chatuikit.extensions.getLifecycleScope
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StickyDateHeaderView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: SceytItemMessageDateSeparatorBinding
    private var showAnimation: ValueAnimator? = null
    private var hideAnimation: ValueAnimator? = null
    private val lifecycleScope by lazy { getLifecycleScope() }
    private var autoHideJob: Job? = null
    private var currentDay: CharSequence = ""

    init {
        binding = SceytItemMessageDateSeparatorBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setDate(date: CharSequence) {
        if (currentDay == date)
            return

        binding.messageDay.text = date
        currentDay = date
    }

    private fun startAutoHideTimer() {
        autoHideJob?.cancel()
        autoHideJob = lifecycleScope?.launch(Dispatchers.Main) {
            delay(1000)
            if (isActive)
                hideVide()
        }
    }

    private fun hideVide() {
        showAnimation?.cancel()
        if (hideAnimation?.isRunning == true)
            return

        hideAnimation = binding.root.changeAlphaWithValueAnim(binding.root.alpha, 0f, 200)
        hideAnimation?.start()
    }

    fun showWithAnim(withAutoHide: Boolean = true) {
        autoHideJob?.cancel()
        hideAnimation?.cancel()

        if (showAnimation?.isRunning != true)
            showAnimation = binding.root.changeAlphaWithValueAnim(binding.root.alpha, 1f, 200)

        if (withAutoHide)
            startAutoHideTimer()
        else autoHideJob?.cancel()
    }

    fun startAutoHide() {
        if (autoHideJob?.isActive == true) return
        startAutoHideTimer()
    }

    internal fun setStyle(listViewStyle: MessagesListViewStyle) {
        val style = listViewStyle.dateSeparatorStyle
        with(binding) {
            style.textStyle.apply(messageDay)

            if (style.backgroundColor != UNSET_COLOR)
                messageDay.setBackgroundTint(style.backgroundColor)
        }
    }
}