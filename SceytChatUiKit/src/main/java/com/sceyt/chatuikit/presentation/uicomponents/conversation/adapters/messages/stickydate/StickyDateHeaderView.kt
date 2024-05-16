package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.stickydate

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.sceyt.chatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.chatuikit.extensions.changeAlphaWithValueAnim
import com.sceyt.chatuikit.extensions.getLifecycleScope
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StickyDateHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var binding: SceytItemMessageDateSeparatorBinding
    private var showAnimation: ValueAnimator? = null
    private var hideAnimation: ValueAnimator? = null
    private val lifecycleScope by lazy { getLifecycleScope() }
    private var autoHideJob: Job? = null
    private var currentDay: String = ""

    init {
        binding = SceytItemMessageDateSeparatorBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setDate(date: String) {
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

    internal fun setStyle(style: MessagesListViewStyle) {
        with(binding) {
            messageDay.apply {
                backgroundTintList = ColorStateList.valueOf(style.dateSeparatorItemBackgroundColor)
                setTextColor(style.dateSeparatorItemTextColor)
                val dateTypeface = if (style.dateSeparatorTextFont != -1)
                    ResourcesCompat.getFont(context, style.dateSeparatorTextFont) else typeface
                setTypeface(dateTypeface, style.dateSeparatorTextStyle)
            }
        }
    }
}