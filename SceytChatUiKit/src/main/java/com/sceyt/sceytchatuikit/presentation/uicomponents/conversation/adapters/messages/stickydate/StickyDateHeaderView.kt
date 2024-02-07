package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.stickydate

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.sceyt.sceytchatuikit.databinding.SceytItemMessageDateSeparatorBinding
import com.sceyt.sceytchatuikit.extensions.changeAlphaWithValueAnim
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.getLifecycleScope
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle
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
    private var currentDay: String? = null

    init {
        binding = SceytItemMessageDateSeparatorBinding.inflate(LayoutInflater.from(context), this, true)
        binding.setMessageItemStyle()
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

        hideAnimation = binding.root.changeAlphaWithValueAnim(binding.root.alpha, 0f, 100)
        hideAnimation?.duration = 100
        hideAnimation?.start()
    }

    fun showWithAnim(withAutoHide: Boolean = true) {
        autoHideJob?.cancel()
        hideAnimation?.cancel()

        if (showAnimation?.isRunning != true)
            showAnimation = binding.root.changeAlphaWithValueAnim(binding.root.alpha, 1f, 100)

        if (withAutoHide)
            startAutoHideTimer()
        else autoHideJob?.cancel()
    }

    private fun checkMaybeNeedToBeUpdated() {
        with(binding.messageDay) {
            val mesW = paint.measureText(currentDay) + paddingStart + paddingEnd
            if (mesW.toInt() != width)
                post { text = currentDay }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        checkMaybeNeedToBeUpdated()
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