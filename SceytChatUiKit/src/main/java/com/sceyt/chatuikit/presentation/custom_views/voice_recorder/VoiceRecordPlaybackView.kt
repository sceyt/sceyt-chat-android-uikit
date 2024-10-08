package com.sceyt.chatuikit.presentation.custom_views.voice_recorder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytVoiceRecordPresenterBinding
import com.sceyt.chatuikit.extensions.TAG_REF
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.mediaPlayerPositionToSeekBarProgress
import com.sceyt.chatuikit.extensions.progressToMediaPlayerPosition
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.media.audio.AudioPlayer
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.chatuikit.styles.input.VoiceRecordPlaybackViewStyle
import java.io.File

class VoiceRecordPlaybackView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private lateinit var style: VoiceRecordPlaybackViewStyle
    private val binding: SceytVoiceRecordPresenterBinding
    var isShowing = false
        private set

    init {
        binding = SceytVoiceRecordPresenterBinding.inflate(LayoutInflater.from(context), this)
    }

    fun init(file: File, audioMetadata: AudioMetadata, listener: VoiceRecordPlaybackListeners? = null) {
        isShowing = true
        with(binding) {
            deleteVoiceRecord.setOnClickListener {
                AudioPlayerHelper.stop(file.path)
                isShowing = false
                listener?.onDeleteVoiceRecord()
            }

            playVoiceRecord.setOnClickListener {
                onPlayVoiceRecordClick(file, audioMetadata, listener)
            }

            icSendMessage.setOnClickListener {
                AudioPlayerHelper.stop(file.path)
                isShowing = false
                listener?.onSendVoiceMessage()
            }

            audioMetadata.tmb?.let { waveformSeekBar.setSampleFrom(it) }
            voiceRecordDuration.text = audioMetadata.dur.times(1000).toLong().durationToMinSecShort()
        }
    }

    private fun SceytVoiceRecordPresenterBinding.onPlayVoiceRecordClick(
            file: File,
            audioMetadata: AudioMetadata,
            listener: VoiceRecordPlaybackListeners? = null
    ) {
        listener?.onPlayVoiceRecord()

        waveformSeekBar.onProgressChanged = object : SeekBarOnProgressChanged {
            override fun onProgressChanged(waveformSeekBar: WaveformSeekBar, progress: Float, fromUser: Boolean) {
                if (fromUser) {
                    val seekPosition = progressToMediaPlayerPosition(progress, audioMetadata.dur.times(1000L))
                    AudioPlayerHelper.seek(file.path, seekPosition)
                }
            }
        }

        AudioPlayerHelper.init(file.path, object : OnAudioPlayer {
            override fun onInitialized(alreadyInitialized: Boolean, player: AudioPlayer, filePath: String) {
                AudioPlayerHelper.toggle(file.path)
            }

            override fun onProgress(position: Long, duration: Long, filePath: String) {
                val seekBarProgress = mediaPlayerPositionToSeekBarProgress(position, duration)
                root.post {
                    waveformSeekBar.progress = seekBarProgress
                    voiceRecordDuration.text = position.durationToMinSecShort()
                }
            }

            override fun onSeek(position: Long, filePath: String) {
            }

            override fun onToggle(playing: Boolean, filePath: String) {
                root.post { setPlayButtonIcon(playing) }
            }

            override fun onStop(filePath: String) {
                root.post {
                    setPlayButtonIcon(false)
                    waveformSeekBar.progress = 0f
                }
            }

            override fun onPaused(filePath: String) {
                root.post { setPlayButtonIcon(false) }
            }

            override fun onSpeedChanged(speed: Float, filePath: String) {
            }

            override fun onError(filePath: String) {
            }
        }, TAG_REF)
    }

    private fun setPlayButtonIcon(playing: Boolean) {
        val icon = if (playing) style.pauseIcon else style.playIcon
        binding.playVoiceRecord.setImageDrawable(icon)
    }

    internal fun setStyle(style: VoiceRecordPlaybackViewStyle) {
        this.style = style
        with(binding) {
            root.setBackgroundColor(style.backgroundColor)
            layoutVoiceRecord.setBackgroundTint(style.playerBackgroundColor)
            icSendMessage.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
            waveformSeekBar.waveBackgroundColor = style.audioWaveformStyle.trackColor
            waveformSeekBar.waveProgressColor = style.audioWaveformStyle.progressColor
            deleteVoiceRecord.setImageDrawable(style.closeIcon)
            icSendMessage.setImageDrawable(style.sendVoiceIcon)
            style.durationTextStyle.apply(voiceRecordDuration)
        }
    }

    interface VoiceRecordPlaybackListeners {
        fun onDeleteVoiceRecord() {}
        fun onPlayVoiceRecord() {}
        fun onSendVoiceMessage() {}
    }
}