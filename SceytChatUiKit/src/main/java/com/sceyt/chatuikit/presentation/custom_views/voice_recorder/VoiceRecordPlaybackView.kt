package com.sceyt.chatuikit.presentation.custom_views.voice_recorder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chatuikit.databinding.SceytVoiceRecordPresenterBinding
import com.sceyt.chatuikit.extensions.TAG_REF
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.mediaPlayerPositionToSeekBarProgress
import com.sceyt.chatuikit.extensions.progressToMediaPlayerPosition
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.media.audio.AudioPlaybackState
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import com.sceyt.chatuikit.styles.input.MessageInputStyle
import com.sceyt.chatuikit.styles.input.VoiceRecordPlaybackViewStyle
import java.io.File

@Suppress("JoinDeclarationAndAssignment")
class VoiceRecordPlaybackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private lateinit var style: VoiceRecordPlaybackViewStyle
    private val binding: SceytVoiceRecordPresenterBinding
    private val messageTid: Long = -1
    var isShowing = false
        private set
    var isViewOnce: Boolean = false
        private set
    var enableViewOnce: Boolean = true

    init {
        binding = SceytVoiceRecordPresenterBinding.inflate(LayoutInflater.from(context), this)
    }

    fun init(
        file: File,
        audioMetadata: AudioMetadata,
        viewOnceSelected: Boolean = false,
        listener: VoiceRecordPlaybackListeners? = null
    ) {
        isShowing = true
        isViewOnce = viewOnceSelected
        with(binding) {
            deleteVoiceRecord.setOnClickListener {
                AudioPlayerHelper.stop(file.path, messageTid)
                isShowing = false
                isViewOnce = false
                listener?.onDeleteVoiceRecord()
            }

            playVoiceRecord.setOnClickListener {
                if (AudioPlayerHelper.alreadyInitialized(file.path, messageTid)) {
                    AudioPlayerHelper.toggle(file.path, messageTid)
                } else {
                    onPlayVoiceRecordClick(file, audioMetadata)
                }
            }

            icSendMessage.setOnClickListener {
                AudioPlayerHelper.stop(file.path, messageTid)
                isShowing = false
                listener?.onSendVoiceMessage(isViewOnce)
                isViewOnce = false
            }

            icViewOnce.setOnClickListener {
                toggleViewOnce()
            }

            icViewOnce.isVisible = enableViewOnce
            updateViewOnceIcon()

            audioMetadata.tmb?.let { waveformSeekBar.setSampleFrom(it) }
            voiceRecordDuration.text =
                audioMetadata.dur.times(1000).toLong().durationToMinSecShort()

            setPlayedState(file, audioMetadata)
        }
    }

    private fun toggleViewOnce() {
        isViewOnce = !isViewOnce
        updateViewOnceIcon()
    }

    private fun updateViewOnceIcon() {
        with(binding.icViewOnce) {
            if (isViewOnce) {
                setImageDrawable(style.viewOnceSelectedIcon)
            } else {
                setImageDrawable(style.viewOnceIcon)
            }
        }
    }

    private fun setPlayedState(
        file: File,
        audioMetadata: AudioMetadata,
    ) {
        val savedState = AudioPlayerHelper.getPlaybackState(
            filePath = file.path ?: return,
            messageTid = messageTid
        )
        if (savedState != null) {
            // Restore the saved position and speed in UI
            binding.waveformSeekBar.progress = mediaPlayerPositionToSeekBarProgress(
                savedState.position, audioMetadata.dur.times(1000L)
            )
        }
    }

    private fun SceytVoiceRecordPresenterBinding.onPlayVoiceRecordClick(
        file: File,
        audioMetadata: AudioMetadata
    ) {
        waveformSeekBar.onProgressChanged = object : SeekBarOnProgressChanged {
            override fun onProgressChanged(
                waveformSeekBar: WaveformSeekBar,
                progress: Float,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val seekPosition = progressToMediaPlayerPosition(
                        progress = progress,
                        mediaDuration = audioMetadata.dur.times(1000L)
                    )
                    AudioPlayerHelper.seek(
                        filePath = file.path,
                        messageTid = messageTid,
                        position = seekPosition
                    )
                }
            }
        }

        AudioPlayerHelper.init(
            filePath = file.path,
            messageTid = messageTid,
            events = object : OnAudioPlayer {
                override fun onProgress(
                    position: Long, duration: Long, filePath: String,
                    messageTid: MessageTid
                ) {
                    val seekBarProgress = mediaPlayerPositionToSeekBarProgress(position, duration)
                    root.post {
                        waveformSeekBar.progress = seekBarProgress
                        voiceRecordDuration.text = position.durationToMinSecShort()
                    }
                }

                override fun onToggle(
                    playing: Boolean, filePath: String,
                    messageTid: MessageTid
                ) {
                    root.post { setPlayButtonIcon(playing) }
                }

                override fun onStop(
                    filePath: String,
                    messageTid: MessageTid,
                    savedState: AudioPlaybackState?
                ) {
                    root.post {
                        setPlayButtonIcon(false)
                        waveformSeekBar.progress = 0f
                    }
                }

                override fun onPaused(
                    filePath: String,
                    messageTid: MessageTid
                ) {
                    root.post { setPlayButtonIcon(false) }
                }
            },
            tag = TAG_REF
        )
    }

    private fun setPlayButtonIcon(playing: Boolean) {
        val icon = if (playing) style.pauseIcon else style.playIcon
        binding.playVoiceRecord.setImageDrawable(icon)
    }

    internal fun setStyle(inputStyle: MessageInputStyle) {
        this.style = inputStyle.voiceRecordPlaybackViewStyle
        with(binding) {
            root.setBackgroundColor(style.backgroundColor)
            layoutVoiceRecord.setBackgroundTint(style.playerBackgroundColor)
            icSendMessage.setBackgroundTint(inputStyle.sendIconBackgroundColor)
            waveformSeekBar.waveBackgroundColor = style.audioWaveformStyle.trackColor
            waveformSeekBar.waveProgressColor = style.audioWaveformStyle.progressColor
            deleteVoiceRecord.setImageDrawable(style.closeIcon)
            icSendMessage.setImageDrawable(style.sendVoiceIcon)
            icViewOnce.setImageDrawable(style.viewOnceIcon)
            style.durationTextStyle.apply(voiceRecordDuration)
        }
    }

    interface VoiceRecordPlaybackListeners {
        fun onDeleteVoiceRecord() {}
        fun onSendVoiceMessage(isViewOnce: Boolean) {}
    }
}