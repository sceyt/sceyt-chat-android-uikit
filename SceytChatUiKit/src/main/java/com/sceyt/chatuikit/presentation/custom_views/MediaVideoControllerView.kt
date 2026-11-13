package com.sceyt.chatuikit.presentation.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import com.sceyt.chatuikit.databinding.SceytMediaVideoControllerViewBinding
import com.sceyt.chatuikit.styles.common.TextStyle
import java.util.Formatter
import java.util.Locale

@OptIn(UnstableApi::class)
internal class MediaVideoControllerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = SceytMediaVideoControllerViewBinding.inflate(LayoutInflater.from(context), this)
    private val timeBuilder = StringBuilder()
    private val timeFormatter = Formatter(timeBuilder, Locale.getDefault())
    private var player: Player? = null
    private var playIcon: Drawable? = null
    private var pauseIcon: Drawable? = null
    private var isScrubbing = false
    private var wasPlayingBeforeScrub = false
    private var shownPositionSec = -1L

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateAll()
        }
    }

    private val progressTicker = object : Runnable {
        override fun run() {
            updateProgress()
            if (shouldTickProgress()) postOnAnimation(this)
        }
    }

    var isControllerVisible = isVisible
        private set

    init {
        isClickable = true
        isFocusable = true
        binding.playPause.setOnClickListener {
            player?.let { Util.handlePlayPauseButtonAction(it) }
        }
        initSeekBar()
    }

    fun setControllerVisible(visible: Boolean, animate: Boolean) {
        isControllerVisible = visible
        animate().cancel()
        if (!animate || !isAttachedToWindow) {
            alpha = 1f
            isVisible = visible
            return
        }
        if (visible) {
            if (!isVisible) {
                alpha = 0f
                isVisible = true
            }
            animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
        } else {
            animate().alpha(0f).setDuration(FADE_DURATION_MS).withEndAction {
                isVisible = false
                alpha = 1f
            }.start()
        }
    }

    fun setPlayer(player: Player?) {
        if (this.player === player) return
        this.player?.removeListener(playerListener)
        this.player = player
        player?.addListener(playerListener)
        shownPositionSec = -1L
        updateAll()
    }

    fun applyStyle(
        @ColorInt backgroundColor: Int,
        playIcon: Drawable?,
        pauseIcon: Drawable?,
        @ColorInt trackColor: Int,
        @ColorInt progressColor: Int,
        @ColorInt thumbColor: Int,
        timelineTextStyle: TextStyle,
    ) {
        setBackgroundColor(backgroundColor)
        this.playIcon = playIcon
        this.pauseIcon = pauseIcon
        with(binding.seekBar) {
            progressBackgroundTintList = ColorStateList.valueOf(trackColor)
            progressTintList = ColorStateList.valueOf(progressColor)
            thumbTintList = ColorStateList.valueOf(thumbColor)
        }
        timelineTextStyle.apply(binding.tvPosition)
        timelineTextStyle.apply(binding.tvDuration)
        updatePlayPause()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSeekBar() = with(binding.seekBar) {
        setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN)
                view.parent?.requestDisallowInterceptTouchEvent(true)
            false
        }
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isScrubbing = true
                wasPlayingBeforeScrub = player?.playWhenReady == true
                player?.pause()
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                showPosition(progress.toLong())
                if (!isScrubbing) player?.seekTo(progress.toLong())
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isScrubbing = false
                val player = player ?: return
                player.seekTo(seekBar.progress.toLong())
                if (wasPlayingBeforeScrub) player.play()
            }
        })
    }

    private fun updateAll() {
        updatePlayPause()
        updateDuration()
        updateProgress()
        scheduleProgressTick()
    }

    private fun updatePlayPause() {
        val showPlay = player?.let { Util.shouldShowPlayButton(it) } ?: true
        binding.playPause.setImageDrawable(if (showPlay) playIcon else pauseIcon)
    }

    private fun updateDuration() {
        val durationMs = player?.duration?.takeIf { it != C.TIME_UNSET } ?: 0L
        binding.seekBar.max = durationMs.toProgress()
        binding.tvDuration.text = formatTime(durationMs)
    }

    private fun updateProgress() {
        if (isScrubbing) return
        val positionMs = player?.currentPosition ?: 0L
        binding.seekBar.progress = positionMs.toProgress()
        showPosition(positionMs)
    }

    private fun showPosition(positionMs: Long) {
        val positionSec = (positionMs + 500) / 1000
        if (positionSec == shownPositionSec) return
        shownPositionSec = positionSec
        binding.tvPosition.text = formatTime(positionMs)
    }

    private fun shouldTickProgress(): Boolean {
        return isAttachedToWindow && isShown && player?.isPlaying == true
    }

    private fun scheduleProgressTick() {
        removeCallbacks(progressTicker)
        if (shouldTickProgress()) postOnAnimation(progressTicker)
    }

    private fun formatTime(timeMs: Long): String {
        return Util.getStringForTime(timeBuilder, timeFormatter, timeMs)
    }

    private fun Long.toProgress(): Int {
        return coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAll()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(progressTicker)
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) updateAll() else removeCallbacks(progressTicker)
    }

    private companion object {
        const val FADE_DURATION_MS = 300L
    }
}
