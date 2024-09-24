package com.sceyt.chatuikit.presentation.custom_views.voice_recorder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnClickListener
import androidx.constraintlayout.widget.ConstraintLayout
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytVoiceRecordPresenterBinding
import com.sceyt.chatuikit.extensions.TAG_REF
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.mediaPlayerPositionToSeekBarProgress
import com.sceyt.chatuikit.extensions.progressToMediaPlayerPosition
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.extensions.setPlayButtonIcon
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.media.audio.AudioPlayer
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.chatuikit.styles.MessageInputStyle
import java.io.File

class VoiceRecordPlaybackView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: SceytVoiceRecordPresenterBinding
    var isShowing = false
        private set

    init {
        binding = SceytVoiceRecordPresenterBinding.inflate(LayoutInflater.from(context), this)
    }

    fun init(file: File, audioMetadata: AudioMetadata, listener: VoiceRecordPlaybackListeners? = null) {
        isShowing = true
        binding.apply {
            val onClickListener = OnClickListener {
                when (it.id) {
                    deleteVoiceRecord.id -> {
                        AudioPlayerHelper.stop(file.path)
                        isShowing = false
                        listener?.onDeleteVoiceRecord()
                    }

                    playVoiceRecord.id -> {
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
                                root.post { setPlayButtonIcon(playing, playVoiceRecord) }
                            }

                            override fun onStop(filePath: String) {
                                root.post {
                                    setPlayButtonIcon(false, playVoiceRecord)
                                    waveformSeekBar.progress = 0f
                                }
                            }

                            override fun onPaused(filePath: String) {
                                root.post { setPlayButtonIcon(false, playVoiceRecord) }
                            }

                            override fun onSpeedChanged(speed: Float, filePath: String) {
                            }

                            override fun onError(filePath: String) {
                            }
                        }, TAG_REF)
                    }

                    icSendMessage.id -> {
                        AudioPlayerHelper.stop(file.path)
                        isShowing = false
                        listener?.onSendVoiceMessage()
                    }
                }
            }
            deleteVoiceRecord.setOnClickListener(onClickListener)
            playVoiceRecord.setOnClickListener(onClickListener)
            icSendMessage.setOnClickListener(onClickListener)
            audioMetadata.tmb?.let { waveformSeekBar.setSampleFrom(it) }
            voiceRecordDuration.text = audioMetadata.dur.times(1000).toLong().durationToMinSecShort()
        }
    }

    internal fun setStyle(style: MessageInputStyle) {
        with(binding) {
            root.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.backgroundColor))
            icSendMessage.setImageDrawable(style.sendMessageIcon)
            voiceRecordDuration.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
            icSendMessage.setBackgroundTintColorRes(SceytChatUIKit.theme.accentColor)
            waveformSeekBar.waveProgressColor = getCompatColor(SceytChatUIKit.theme.accentColor)
            playVoiceRecord.setTintColorRes(SceytChatUIKit.theme.iconSecondaryColor)
            deleteVoiceRecord.setTintColorRes(SceytChatUIKit.theme.iconSecondaryColor)
            layoutVoiceRecord.setBackgroundTintColorRes(SceytChatUIKit.theme.surface1Color)
        }
    }

    interface VoiceRecordPlaybackListeners {
        fun onDeleteVoiceRecord() {}
        fun onPlayVoiceRecord() {}
        fun onSendVoiceMessage() {}
    }
}