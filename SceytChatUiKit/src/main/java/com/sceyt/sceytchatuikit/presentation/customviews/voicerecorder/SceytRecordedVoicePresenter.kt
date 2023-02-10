package com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.ImageView
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytRecordedVoicePresenterBinding
import com.sceyt.sceytchatuikit.extensions.durationToMinSecShort
import com.sceyt.sceytchatuikit.media.audio.AudioPlayerHelper
import com.sceyt.sceytchatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.sceytchatuikit.sceytconfigs.MessageInputViewStyle
import java.io.File

class SceytRecordedVoicePresenter @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: SceytRecordedVoicePresenterBinding

    init {
        binding = SceytRecordedVoicePresenterBinding.inflate(LayoutInflater.from(context), this, true)
        binding.setupStyle()
    }

    fun init(file: File, audioMetadata: AudioMetadata, listener: RecordedVoicePresentListeners? = null) {
        binding.apply {
            val onClickListener = OnClickListener {
                when (it.id) {
                    deleteVoiceRecord.id -> {
                        listener?.onDeleteVoiceRecord()
                        AudioPlayerHelper.stop(file.path)
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
                            override fun onInitialized() {
                                AudioPlayerHelper.toggle(file.path)
                            }

                            override fun onProgress(position: Long, duration: Long) {
                                val seekBarProgress = mediaPlayerPositionToSeekBarProgress(position, duration)
                                root.post {
                                    waveformSeekBar.progress = seekBarProgress
                                    voiceRecordDuration.text = position.durationToMinSecShort()
                                }
                            }

                            override fun onSeek(position: Long) {
                            }

                            override fun onToggle(playing: Boolean) {
                                root.post { setPlayButtonIcon(playing, playVoiceRecord) }
                            }

                            override fun onStop() {
                                root.post {
                                    setPlayButtonIcon(false, playVoiceRecord)
                                    waveformSeekBar.progress = 0f
                                }
                            }

                            override fun onSpeedChanged(speed: Float) {
                            }

                            override fun onError() {
                            }
                        })
                    }
                    icSendMessage.id -> {
                        AudioPlayerHelper.stop(file.path)
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

    private fun progressToMediaPlayerPosition(progress: Float, mediaDuration: Long): Long {
        return ((progress * mediaDuration) / 100f).toLong()
    }

    private fun mediaPlayerPositionToSeekBarProgress(currentPosition: Long, mediaDuration: Long): Float {
        return (currentPosition * 100f / mediaDuration)
    }

    private fun setPlayButtonIcon(playing: Boolean, imageView: ImageView) {
        val iconRes = if (playing) R.drawable.sceyt_ic_pause else R.drawable.sceyt_ic_play
        imageView.setImageResource(iconRes)
    }

    private fun SceytRecordedVoicePresenterBinding.setupStyle() {
        icSendMessage.setImageResource(MessageInputViewStyle.sendMessageIcon)
    }

    interface RecordedVoicePresentListeners {
        fun onDeleteVoiceRecord() {}
        fun onPlayVoiceRecord() {}
        fun onSendVoiceMessage() {}
    }
}