package com.sceyt.chatuikit.presentation.components.channel.messages.preview

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.Gson
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytActivitySelfDestructingVoiceMessageBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.applySystemBarsStyle
import com.sceyt.chatuikit.extensions.createIntent
import com.sceyt.chatuikit.extensions.darkModeContext
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.mediaPlayerPositionToSeekBarProgress
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.progressToMediaPlayerPosition
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerState
import com.sceyt.chatuikit.media.audio.AudioPlayerStatus
import com.sceyt.chatuikit.media.audio.VoiceStateCoordinator
import com.sceyt.chatuikit.presentation.common.dialogs.ViewOnceInfoDialog
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PlaybackSpeed
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata
import com.sceyt.chatuikit.presentation.extensions.setChatMessageDateAndStatusIcon
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import com.sceyt.chatuikit.styles.preview.SelfDestructingVoiceMessageStyle
import kotlinx.coroutines.launch
import java.util.Date

class SelfDestructingVoiceMessageActivity : AppCompatActivity(), SceytKoinComponent {

    private lateinit var binding: SceytActivitySelfDestructingVoiceMessageBinding
    private lateinit var style: SelfDestructingVoiceMessageStyle
    private lateinit var messageItemStyle: MessageItemStyle

    private val viewModel: SelfDestructingVoiceMessageViewModel by viewModels {
        val message = intent.parcelable<SceytMessage>(MESSAGE_KEY)
            ?: throw IllegalArgumentException("Message is required")
        SelfDestructingVoiceMessageViewModelFactory(message)
    }

    private lateinit var message: SceytMessage
    private var attachment: SceytAttachment? = null

    private var voiceFilePath: String? = null
    private var voiceDuration: Long = 0
    private var currentPlaybackSpeed: PlaybackSpeed = PlaybackSpeed.X1
    private var voiceMessageTid: Long = -1
    private var isIncomingMessage: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        binding = SceytActivitySelfDestructingVoiceMessageBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        applyInsetsAndWindowColor(binding.root)
        applySystemBarsStyle(isDarkMode = true)

        getMessageItemStyle()
        style = SelfDestructingVoiceMessageStyle.Builder(this, messageItemStyle).build()
        binding.applyStyle()
        initViews()
        getBundleArguments()

        displayVoiceMessage()
        observePlaybackState()
        observeMessageUpdates()
        viewModel.sendOpenedMarker(message)
    }

    private fun initViews() {
        binding.toolbar.setNavigationClickListener { finish() }
        binding.toolbar.setMenuClickListener { onMenuItemClick(it) }
    }

    private fun getBundleArguments() {
        message = requireNotNull(intent.parcelable(MESSAGE_KEY))
        attachment = intent.parcelable(ATTACHMENT_KEY)
    }

    private fun getMessageItemStyle() {
        val styleId = intent.getStringExtra(STYLE_ID_KEY)
        messageItemStyle = StyleRegistry.getOrDefault(styleId) {
            MessageItemStyle.Builder(this, null).build()
        }
    }

    private fun observeMessageUpdates() {
        lifecycleScope.launch {
            viewModel.messageUpdatedFlow.collect { updatedMessage ->
                message = updatedMessage
                setMessageDateAndStatus(updatedMessage)
            }
        }
    }

    private fun displayVoiceMessage() {
        val attach = attachment ?: return

        voiceFilePath = attach.filePath ?: attach.url ?: return

        voiceMessageTid = attach.messageTid

        isIncomingMessage = message.incoming

        val audioMetadata = try {
            Gson().fromJson(attach.metadata, AudioMetadata::class.java)
        } catch (_: Exception) {
            AudioMetadata(intArrayOf(0), 0)
        }

        voiceDuration = audioMetadata?.dur?.times(1000L) ?: 0
        audioMetadata?.tmb?.let { binding.voiceWaveformSeekBar.setSampleFrom(it) }
        binding.voiceDuration.text = voiceDuration.durationToMinSecShort()

        applyVoicePlayerStyle()

        binding.ivVoiceViewOnceIcon.isVisible = true

        setMessageDateAndStatus(message)

        setupToolbar()
        setupVoicePlayerControls()
    }

    private fun setupToolbar() {
        val attach = attachment ?: return

        val userName = message.user?.let { messageItemStyle.senderNameFormatter.format(this, it) }
            ?: getString(R.string.sceyt_view_once_message)
        binding.toolbar.setTitle(userName)

        val formattedDate =
            messageItemStyle.messageDateFormatter.format(this, Date(attach.createdAt))
        binding.toolbar.setSubtitle(formattedDate)
    }

    private fun applyVoicePlayerStyle() {
        val bubbleBackgroundStyle = if (isIncomingMessage) {
            messageItemStyle.incomingBubbleBackgroundStyle
        } else {
            messageItemStyle.outgoingBubbleBackgroundStyle
        }
        bubbleBackgroundStyle.apply(binding.voicePlayerContainer)

        binding.voicePlayPauseButton.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)

        binding.voiceWaveformSeekBar.apply {
            waveProgressColor = messageItemStyle.voiceWaveformStyle.progressColor
            waveBackgroundColor = messageItemStyle.voiceWaveformStyle.trackColor
        }

        messageItemStyle.voiceSpeedTextStyle.apply(binding.voicePlaybackSpeed)
        binding.voicePlaybackSpeed.text = currentPlaybackSpeed.displayValue
        messageItemStyle.voiceDurationTextStyle.apply(binding.voiceDuration)
        messageItemStyle.viewOnceBadgeStyle.apply(binding.ivVoiceViewOnceIcon)
    }

    private fun setMessageDateAndStatus(message: SceytMessage) {
        val dateText = messageItemStyle.messageDateFormatter.format(this, Date(message.createdAt))
        val isEdited = message.state == MessageState.Edited

        message.setChatMessageDateAndStatusIcon(
            binding.voiceMessageDate,
            messageItemStyle,
            dateText,
            isEdited
        )
    }

    private fun setupVoicePlayerControls() {
        binding.voicePlayPauseButton.setOnClickListener {
            toggleVoicePlayback()
        }

        binding.voicePlaybackSpeed.setOnClickListener {
            val nextSpeed = currentPlaybackSpeed.next()
            currentPlaybackSpeed = nextSpeed
            binding.voicePlaybackSpeed.text = nextSpeed.displayValue
            AudioPlayerHelper.setPlaybackSpeed(
                filePath = voiceFilePath,
                messageTid = voiceMessageTid,
                speed = nextSpeed.value
            )
        }

        binding.voiceWaveformSeekBar.onProgressChanged = object : SeekBarOnProgressChanged {
            override fun onProgressChanged(
                waveformSeekBar: WaveformSeekBar,
                progress: Float,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val seekPosition = progressToMediaPlayerPosition(progress, voiceDuration)
                    AudioPlayerHelper.seek(
                        filePath = voiceFilePath,
                        messageTid = voiceMessageTid,
                        position = seekPosition
                    )
                }
            }
        }

        voiceFilePath?.let { filePath ->
            val isPlaying = AudioPlayerHelper.isPlaying(filePath, voiceMessageTid)
            binding.voiceWaveformSeekBar.isEnabled = isPlaying
            binding.voicePlaybackSpeed.isEnabled = isPlaying
        }
    }

    private fun toggleVoicePlayback() {
        val filePath = voiceFilePath ?: return

        VoiceStateCoordinator.stopRecordingIfActive()

        if (AudioPlayerHelper.alreadyInitialized(filePath, voiceMessageTid)) {
            AudioPlayerHelper.toggle(filePath, voiceMessageTid)
        } else {
            initVoiceAudioPlayer()
        }
    }

    private fun initVoiceAudioPlayer() {
        val filePath = voiceFilePath ?: return
        AudioPlayerHelper.init(
            filePath = filePath,
            messageTid = voiceMessageTid
        )
    }

    private fun observePlaybackState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AudioPlayerHelper.state.collect(::onPlaybackStateChanged)
            }
        }
    }

    private fun onPlaybackStateChanged(state: AudioPlayerState) {
        if (!state.matches(voiceFilePath, voiceMessageTid)) {
            setVoicePlayButtonIcon(false)
            return
        }

        setVoicePlayButtonIcon(state.isPlaying)
        currentPlaybackSpeed = PlaybackSpeed.fromValue(state.speed)
        binding.voicePlaybackSpeed.text = currentPlaybackSpeed.displayValue

        val duration = state.duration.takeIf { it > 0 } ?: voiceDuration
        binding.voiceWaveformSeekBar.progress = mediaPlayerPositionToSeekBarProgress(
            state.position,
            duration
        )
        binding.voiceDuration.text = if (state.status == AudioPlayerStatus.Completed) {
            voiceDuration.durationToMinSecShort()
        } else {
            state.position.durationToMinSecShort()
        }
        val controlsEnabled = state.status == AudioPlayerStatus.Playing ||
                state.status == AudioPlayerStatus.Paused ||
                state.status == AudioPlayerStatus.Initializing
        binding.voiceWaveformSeekBar.isEnabled = controlsEnabled
        binding.voicePlaybackSpeed.isEnabled = controlsEnabled
        if (state.status == AudioPlayerStatus.Completed) {
            binding.voiceWaveformSeekBar.progress = 0f
        }
    }

    private fun setVoicePlayButtonIcon(playing: Boolean) {
        val icon: Drawable? = if (playing) {
            getCompatDrawable(R.drawable.sceyt_ic_pause)
        } else {
            getCompatDrawable(R.drawable.sceyt_ic_play)
        }
        binding.voicePlayPauseButton.setImageDrawable(icon)
    }

    private fun onMenuItemClick(itemId: Int) {
        when (itemId) {
            R.id.sceyt_self_destruct_indicator -> {
                showViewOnceInfoDialog()
            }
        }
    }

    private fun showViewOnceInfoDialog() {
        ViewOnceInfoDialog.showDialog(
            context = darkModeContext(),
            acceptListener = {}
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceFilePath?.let {
            AudioPlayerHelper.stop(it, voiceMessageTid)
        }
        StyleRegistry.unregister(intent.getStringExtra(STYLE_ID_KEY))
    }

    private fun SceytActivitySelfDestructingVoiceMessageBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(toolbar)
    }

    companion object {
        private const val MESSAGE_KEY = "message"
        private const val ATTACHMENT_KEY = "attachment"
        private const val STYLE_ID_KEY = "style_id"

        fun createIntent(
            context: Context,
            message: SceytMessage,
            attachment: SceytAttachment,
            styleId: String
        ): Intent = context.createIntent<SelfDestructingVoiceMessageActivity> {
            putExtra(MESSAGE_KEY, message)
            putExtra(ATTACHMENT_KEY, attachment)
            putExtra(STYLE_ID_KEY, styleId)
        }
    }
}
