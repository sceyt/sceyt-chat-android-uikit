package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.stickydate

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewPropertyAnimator
import android.widget.FrameLayout
import com.sceyt.sceytchatuikit.databinding.SceytItemMessageDateSeparatorBinding

class StycyDateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var binding: SceytItemMessageDateSeparatorBinding
    private var animation: ViewPropertyAnimator? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        hideVide()
    }

    init {
        binding = SceytItemMessageDateSeparatorBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setDate(date: String) {
        binding.messageDay.text = date
    }

    fun startAoutHideTImer() {
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, 3000)
    }

    private fun hideVide() {
        animation?.cancel()
        animation = binding.root.animate().alpha(0f)
        animation?.duration = 200
        animation?.start()

        Log.i("sdfssdfsdfdfsd", "hideVide: ")
    }

    fun show() {
        handler.removeCallbacks(runnable)
        animation?.cancel()
        animation = binding.root.animate().alpha(1f)
        animation?.duration = 200
        animation?.start()
        Log.i("sdfssdfsdfdfsd", "show: ")

        startAoutHideTImer()
    }
}