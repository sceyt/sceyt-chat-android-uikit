package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.stickydate

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.sceyt.chatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.chatuikit.extensions.changeAlphaWithValueAnim
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.getLifecycleScope
import com.sceyt.chatuikit.sceytstyles.MessagesStyle
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
        binding.setMessageItemStyle()
    }

    fun setDate(date: String) {
        if (currentDay == date)
            return

        binding.messageDay.text = date
        if (currentDay.length != date.length) {
            binding.root.post {
                checkMaybeNeedRequestLayout()
            }
        }
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

    override fun onLayout(changed: Boolean, l: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, l, top, right, bottom)
        with(binding.messageDay) {
            val mesW = paint.measureText(text.toString()) + paddingStart + paddingEnd
            val left = (this@StickyDateHeaderView.width - mesW) / 2
            layout(left.toInt(), 0, (left + mesW).toInt(), height)
        }
    }

    private fun checkMaybeNeedRequestLayout() {
        with(binding.messageDay) {
            val mesW = paint.measureText(currentDay).toInt() + paddingStart + paddingEnd
            if (mesW != width) {
                requestLayout()
                invalidate()
            }
        }
    }

    private fun SceytItemMessageDateSeparatorBinding.setMessageItemStyle() {
        with(context) {
            messageDay.apply {
                background = getCompatDrawable(MessagesStyle.dateSeparatorItemBackground)
                setTextColor(getCompatColor(MessagesStyle.dateSeparatorItemTextColor))
                val dateTypeface = if (MessagesStyle.dateSeparatorTextFont != -1)
                    ResourcesCompat.getFont(this@with, MessagesStyle.dateSeparatorTextFont) else typeface
                setTypeface(dateTypeface, MessagesStyle.dateSeparatorTextStyle)
            }
        }
    }
}