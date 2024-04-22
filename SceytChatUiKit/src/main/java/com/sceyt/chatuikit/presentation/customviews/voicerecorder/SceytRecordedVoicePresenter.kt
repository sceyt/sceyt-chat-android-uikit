package com.sceyt.chatuikit.presentation.customviews.voicerecorder

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.widget.FrameLayout
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytRecordedVoicePresenterBinding
import com.sceyt.chatuikit.extensions.TAG_REF
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.mediaPlayerPositionToSeekBarProgress
import com.sceyt.chatuikit.extensions.progressToMediaPlayerPosition
import com.sceyt.chatuikit.extensions.setPlayButtonIcon
import com.sceyt.chatuikit.media.audio.AudioPlayer
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.chatuikit.sceytstyles.MessageInputViewStyle
import java.io.File

class SceytRecordedVoicePresenter @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: SceytRecordedVoicePresenterBinding
    var isShowing = false
        private set

    init {
        binding = SceytRecordedVoicePresenterBinding.inflate(LayoutInflater.from(context), this, true)
        binding.setupStyle()
    }

    fun init(file: File, audioMetadata: AudioMetadata, listener: RecordedVoicePresentListeners? = null) {
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

    private fun SceytRecordedVoicePresenterBinding.setupStyle() {
        icSendMessage.setImageResource(MessageInputViewStyle.sendMessageIcon)
        icSendMessage.backgroundTintList = ColorStateList.valueOf(context.getCompatColor(SceytChatUIKit.theme.accentColor))
        waveformSeekBar.waveProgressColor = getCompatColor(SceytChatUIKit.theme.accentColor)
    }

    interface RecordedVoicePresentListeners {
        fun onDeleteVoiceRecord() {}
        fun onPlayVoiceRecord() {}
        fun onSendVoiceMessage() {}
    }
}