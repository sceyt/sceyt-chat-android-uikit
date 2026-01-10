package com.sceyt.chatuikit.presentation.components.channel.messages.preview

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.gson.Gson
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytActivitySelfDestructingMediaPreviewBinding
import com.sceyt.chatuikit.extensions.applySystemWindowInsetsPadding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.mediaPlayerPositionToSeekBarProgress
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.progressToMediaPlayerPosition
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.media.audio.AudioPlayer
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import com.sceyt.chatuikit.presentation.common.dialogs.ViewOnceInfoDialog
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PlaybackSpeed
import com.sceyt.chatuikit.presentation.custom_views.PlayPauseImage
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata
import com.sceyt.chatuikit.presentation.extensions.setChatMessageDateAndStatusIcon
import com.sceyt.chatuikit.presentation.helpers.ExoPlayerHelper
import com.sceyt.chatuikit.styles.preview.SelfDestructingMediaPreviewStyle
import kotlinx.coroutines.launch
import java.util.Date

class SelfDestructingMediaPreviewActivity : AppCompatActivity(), SceytKoinComponent {

    private lateinit var binding: SceytActivitySelfDestructingMediaPreviewBinding
    private lateinit var style: SelfDestructingMediaPreviewStyle

    private val viewModel: SelfDestructingMediaPreviewViewModel by viewModels {
        val message = intent.parcelable<SceytMessage>(MESSAGE_KEY)
            ?: throw IllegalArgumentException("Message is required")
        SelfDestructingMediaPreviewViewModelFactory(message)
    }

    private var message: SceytMessage? = null
    private var attachment: SceytAttachment? = null

    private var playerHelper: ExoPlayerHelper? = null
    private var videoController: ConstraintLayout? = null

    private lateinit var textExpandCollapseHelper: TextExpandCollapseHelper
    private var isVideoAttachment = false
    private var isVoiceAttachment = false
    
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

        style = SelfDestructingMediaPreviewStyle.Builder(this, null).build()
        binding = SceytActivitySelfDestructingMediaPreviewBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        binding.applyStyle()
        initViews()
        getBundleArguments()

        displayMedia()
        observeMessageUpdates()
        viewModel.sendOpenedMarker(message!!)
    }

    private fun initViews() {
        binding.toolbar.applySystemWindowInsetsPadding(applyTop = true, applyRight = true, applyLeft = true)

        binding.messageBodyScrollView.apply {
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        textExpandCollapseHelper = TextExpandCollapseHelper(
            textView = binding.tvMessageBody,
            scrollView = binding.messageBodyScrollView,
            containerView = binding.messageBodyContainer
        )

        binding.root.post { toggleFullScreen(false) }

        binding.toolbar.setNavigationClickListener { finish() }
        binding.toolbar.setMenuClickListener { onMenuItemClick(it) }
        binding.imageView.setOnPhotoTapListener { _, _, _ -> onMediaClick() }

        initVideoController()
    }

    @OptIn(UnstableApi::class)
    private fun initVideoController() {
        binding.videoView.controllerHideOnTouch = false

        binding.videoView.findViewById<ConstraintLayout>(R.id.videoTimeContainer)?.let { controller ->
            videoController = controller
            controller.applySystemWindowInsetsPadding(applyBottom = true, applyRight = true, applyLeft = true)
            controller.isVisible = binding.toolbar.isVisible
        }

        binding.videoView.setOnClickListener {
            onMediaClick()
        }
    }

    private fun getBundleArguments() {
        message = intent.parcelable(MESSAGE_KEY)
        attachment = intent.parcelable(ATTACHMENT_KEY)
    }

    private fun observeMessageUpdates() {
        lifecycleScope.launch {
            viewModel.messageFlow.collect { updatedMessage ->
                message = updatedMessage
                if (isVoiceAttachment) {
                    setMessageDateAndStatus(updatedMessage)
                }
            }
        }
    }

    private fun displayMedia() {
        val attach = attachment ?: return
        val msg = message ?: return

        val userName = msg.user?.let { style.userNameFormatter.format(this, it) }
            ?: getString(R.string.sceyt_view_once_message)
        binding.toolbar.setTitle(userName)

        val formattedDate = style.mediaDateFormatter.format(this, Date(attach.createdAt))
        binding.toolbar.setSubtitle(formattedDate)

        when (attach.type) {
            AttachmentTypeEnum.Image.value -> displayImage(attach)
            AttachmentTypeEnum.Video.value -> displayVideo(attach)
            AttachmentTypeEnum.Voice.value -> displayVoice(attach)
            else -> {
                Toast.makeText(this, R.string.sceyt_unsupported_media, Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        displayMessageBody(msg)
    }

    private fun displayImage(attachment: SceytAttachment) {
        binding.imageView.isVisible = true
        binding.videoView.isVisible = false
        binding.voicePlayerContainer.isVisible = false
        isVideoAttachment = false
        isVoiceAttachment = false

        binding.messageBodyContainer.applySystemWindowInsetsPadding(applyBottom = true)

        val filePath = attachment.filePath ?: attachment.url
        Glide.with(this)
            .load(filePath)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imageView)
    }

    private fun displayVideo(attachment: SceytAttachment) {
        binding.imageView.isVisible = false
        binding.videoView.isVisible = true
        binding.voicePlayerContainer.isVisible = false
        isVideoAttachment = true
        isVoiceAttachment = false

        val filePath = attachment.filePath ?: attachment.url ?: return

        playerHelper?.releasePlayer()
        playerHelper = ExoPlayerHelper(
            context = this,
            playerView = binding.videoView,
            errorListener = { Toast.makeText(this, "Video playback error", Toast.LENGTH_SHORT).show() }
        )
        playerHelper?.setMediaPath(filePath, playVideo = true)
    }
    
    private fun displayVoice(attachment: SceytAttachment) {
        binding.imageView.isVisible = false
        binding.videoView.isVisible = false
        binding.voicePlayerContainer.isVisible = true
        isVideoAttachment = false
        isVoiceAttachment = true

        voiceFilePath = attachment.filePath ?: attachment.url ?: return

        voiceMessageTid = attachment.messageTid

        val msg = message ?: return
        isIncomingMessage = msg.incoming

        val audioMetadata = try {
            Gson().fromJson(attachment.metadata, AudioMetadata::class.java)
        } catch (_: Exception) {
            AudioMetadata(intArrayOf(0), 0)
        }

        voiceDuration = audioMetadata?.dur?.times(1000L) ?: 0
        audioMetadata?.tmb?.let { binding.voiceWaveformSeekBar.setSampleFrom(it) }
        binding.voiceDuration.text = voiceDuration.durationToMinSecShort()

        applyVoicePlayerStyle()

        binding.ivVoiceViewOnceIcon.isVisible = true

        setMessageDateAndStatus(msg)

        setupVoicePlayerControls()
    }

    private fun applyVoicePlayerStyle() {
        val messageStyle = style.messageItemStyle
        val accentColor = getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        val backgroundColor = getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)

        val bubbleBackgroundStyle = if (isIncomingMessage) {
            messageStyle.incomingBubbleBackgroundStyle
        } else {
            messageStyle.outgoingBubbleBackgroundStyle
        }
        bubbleBackgroundStyle.apply(binding.voicePlayerContainer)

        binding.voicePlayPauseButton.setBackgroundTint(accentColor)

        binding.voiceWaveformSeekBar.apply {
            waveProgressColor = messageStyle.voiceWaveformStyle.progressColor
            waveBackgroundColor = messageStyle.voiceWaveformStyle.trackColor
        }

        messageStyle.voiceSpeedTextStyle.apply(binding.voicePlaybackSpeed)
        messageStyle.voiceDurationTextStyle.apply(binding.voiceDuration)

        binding.ivVoiceViewOnceIcon.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(accentColor)
            setStroke(dpToPx(1f), backgroundColor)
        }
        binding.ivVoiceViewOnceIcon.setImageDrawable(messageStyle.viewOnceBadgeIcon)
    }

    private fun setMessageDateAndStatus(message: SceytMessage) {
        val dateText = style.messageItemStyle.messageDateFormatter.format(this, Date(message.createdAt))
        val isEdited = message.state == MessageState.Edited

        message.setChatMessageDateAndStatusIcon(
            binding.voiceMessageDate,
            style.messageItemStyle,
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

            if (AudioPlayerHelper.alreadyInitialized(filePath, voiceMessageTid)) {
                AudioPlayerHelper.getCurrentPlayer()?.addEventListener(
                    event = voicePlayerListener,
                    tag = "SelfDestructingVoice"
                )
            }
        }
    }

    private fun toggleVoicePlayback() {
        val filePath = voiceFilePath ?: return

        if (AudioPlayerHelper.alreadyInitialized(filePath, voiceMessageTid)) {
            AudioPlayerHelper.getCurrentPlayer()?.addEventListener(
                event = voicePlayerListener,
                tag = "SelfDestructingVoice"
            )
            AudioPlayerHelper.toggle(filePath, voiceMessageTid)
        } else {
            initVoiceAudioPlayer()
        }
    }
    
    private fun initVoiceAudioPlayer() {
        val filePath = voiceFilePath ?: return
        AudioPlayerHelper.init(
            filePath = filePath,
            messageTid = voiceMessageTid,
            events = voicePlayerListener,
            tag = "SelfDestructingVoice"
        )
    }
    
    private val voicePlayerListener: AudioPlayerHelper.OnAudioPlayer by lazy {
        object : AudioPlayerHelper.OnAudioPlayer {
            override fun onInitialized(
                alreadyInitialized: Boolean,
                player: AudioPlayer,
                filePath: String,
                messageTid: MessageTid
            ) {
                if (filePath != voiceFilePath || messageTid != voiceMessageTid) return

                if (!alreadyInitialized) {
                    player.togglePlayPause()
                }

                runOnUiThread {
                    binding.voiceWaveformSeekBar.isEnabled = true
                    binding.voicePlaybackSpeed.isEnabled = true
                }
            }

            override fun onProgress(
                position: Long,
                duration: Long,
                filePath: String,
                messageTid: MessageTid
            ) {
                if (filePath != voiceFilePath || messageTid != voiceMessageTid) return

                val seekBarProgress = mediaPlayerPositionToSeekBarProgress(position, duration)
                runOnUiThread {
                    binding.voiceWaveformSeekBar.progress = seekBarProgress
                    binding.voiceWaveformSeekBar.isEnabled = true
                    binding.voicePlaybackSpeed.isEnabled = true
                    binding.voiceDuration.text = position.durationToMinSecShort()
                }
            }

            override fun onToggle(
                playing: Boolean,
                filePath: String,
                messageTid: MessageTid
            ) {
                if (filePath != voiceFilePath || messageTid != voiceMessageTid) return

                runOnUiThread {
                    setVoicePlayButtonIcon(playing)
                }
            }

            override fun onStop(
                filePath: String,
                messageTid: MessageTid
            ) {
                if (filePath != voiceFilePath || messageTid != voiceMessageTid) return

                runOnUiThread {
                    setVoicePlayButtonIcon(false)
                    currentPlaybackSpeed = PlaybackSpeed.X1
                    binding.voiceWaveformSeekBar.progress = 0f
                    binding.voiceDuration.text = voiceDuration.durationToMinSecShort()
                    binding.voiceWaveformSeekBar.isEnabled = false
                    binding.voicePlaybackSpeed.isEnabled = false
                    binding.voicePlaybackSpeed.text = currentPlaybackSpeed.displayValue
                }
            }

            override fun onPaused(
                filePath: String,
                messageTid: MessageTid
            ) {
                if (filePath != voiceFilePath || messageTid != voiceMessageTid) return

                runOnUiThread {
                    setVoicePlayButtonIcon(false)
                }
            }

            override fun onSpeedChanged(
                speed: Float,
                filePath: String,
                messageTid: MessageTid
            ) {
                if (filePath != voiceFilePath || messageTid != voiceMessageTid) return

                runOnUiThread {
                    currentPlaybackSpeed = PlaybackSpeed.fromValue(speed)
                    binding.voicePlaybackSpeed.text = currentPlaybackSpeed.displayValue
                }
            }
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

    private fun displayMessageBody(msg: SceytMessage) {
        val body = msg.body.trim()
        if (body.isBlank()) {
            binding.messageBodyContainer.isVisible = false
            return
        }

        binding.messageBodyContainer.isVisible = true
        textExpandCollapseHelper.setText(body)

        binding.messageBodyContainer.post {
            adjustMessageBodyPosition(videoController?.isVisible == true)
        }
    }

    private fun onMediaClick() {
        val newVisibility = !binding.toolbar.isVisible
        binding.toolbar.isVisible = newVisibility
        videoController?.isVisible = newVisibility

        adjustMessageBodyPosition(newVisibility)

        toggleFullScreen(!newVisibility)
    }

    private fun adjustMessageBodyPosition(controllerVisible: Boolean) {
        if (!isVideoAttachment) return

        val controller = videoController ?: return

        if (controllerVisible) {
            controller.post {
                val controllerHeight = controller.height
                val containerMargin = (binding.messageBodyContainer.layoutParams as? ConstraintLayout.LayoutParams)?.bottomMargin ?: 0
                binding.messageBodyContainer.translationY = -(controllerHeight - containerMargin).toFloat()
            }
        } else {
            binding.messageBodyContainer.translationY = 0f
        }
    }

    private fun toggleFullScreen(isFullScreen: Boolean) {
        WindowInsetsControllerCompat(window, binding.root).apply {
            if (isFullScreen) hide(WindowInsetsCompat.Type.systemBars())
            else show(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
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
            context = this,
            acceptListener = {}
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        playerHelper?.releasePlayer()
        playerHelper = null
        textExpandCollapseHelper.cleanup()
        
        voiceFilePath?.let {
            AudioPlayerHelper.stop(it, voiceMessageTid)
        }
    }

    private fun SceytActivitySelfDestructingMediaPreviewBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(toolbar)
        applyVideoPlayerStyle()
        applyMessageBodyStyle()
    }

    @OptIn(UnstableApi::class)
    private fun SceytActivitySelfDestructingMediaPreviewBinding.applyVideoPlayerStyle() {
        with(videoView) {
            findViewById<View>(R.id.videoTimeContainer)?.setBackgroundColor(style.videoControllerBackgroundColor)

            findViewById<PlayPauseImage>(R.id.exo_play_pause)?.apply {
                setPlayIcon(style.playIcon)
                setPauseIcon(style.pauseIcon)
            }

            findViewById<DefaultTimeBar>(R.id.exo_progress)?.apply {
                setPlayedColor(style.progressColor)
                setScrubberColor(style.thumbColor)
                setUnplayedColor(style.trackColor)
                setBufferedColor(style.trackColor)
            }

            style.timelineTextStyle.apply(findViewById(R.id.exo_position))
            style.timelineTextStyle.apply(findViewById(R.id.tvMiddle))
            style.timelineTextStyle.apply(findViewById(R.id.exo_duration))
        }
    }

    private fun SceytActivitySelfDestructingMediaPreviewBinding.applyMessageBodyStyle() {
        style.messageBodyTextStyle.apply(tvMessageBody)
        style.messageBodyBackgroundStyle.apply(messageBodyContainer)
    }

    companion object {
        private const val MESSAGE_KEY = "message"
        private const val ATTACHMENT_KEY = "attachment"

        fun launchActivity(context: Context, message: SceytMessage, attachment: SceytAttachment) {
            context.launchActivity<SelfDestructingMediaPreviewActivity> {
                putExtra(MESSAGE_KEY, message)
                putExtra(ATTACHMENT_KEY, attachment)
            }
        }
    }
}
