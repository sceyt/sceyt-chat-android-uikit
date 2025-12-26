package com.sceyt.chatuikit.presentation.components.channel.messages.preview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytActivitySelfDestructingMediaPreviewBinding
import com.sceyt.chatuikit.extensions.applySystemWindowInsetsPadding
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.presentation.custom_views.PlayPauseImage
import com.sceyt.chatuikit.presentation.helpers.ExoPlayerHelper
import com.sceyt.chatuikit.styles.preview.SelfDestructingMediaPreviewStyle
import org.koin.core.component.inject
import java.util.Date

class SelfDestructingMediaPreviewActivity : AppCompatActivity(), SceytKoinComponent {

    private lateinit var binding: SceytActivitySelfDestructingMediaPreviewBinding
    private lateinit var style: SelfDestructingMediaPreviewStyle
    private val messageInteractor: MessageInteractor by inject()

    private val viewModel: SelfDestructingMediaPreviewViewModel by viewModels {
        SelfDestructingMediaPreviewViewModelFactory(messageInteractor)
    }

    private var message: SceytMessage? = null
    private var attachment: SceytAttachment? = null

    private var playerHelper: ExoPlayerHelper? = null
    private var videoController: ConstraintLayout? = null

    private lateinit var textExpandCollapseHelper: TextExpandCollapseHelper
    private var isVideoAttachment = false

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
        message?.let { viewModel.sendOpenedMarker(it) }
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
        isVideoAttachment = false

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
        isVideoAttachment = true

        val filePath = attachment.filePath ?: attachment.url ?: return

        playerHelper?.releasePlayer()
        playerHelper = ExoPlayerHelper(
            context = this,
            playerView = binding.videoView,
            errorListener = { Toast.makeText(this, "Video playback error", Toast.LENGTH_SHORT).show() }
        )
        playerHelper?.setMediaPath(filePath, playVideo = true)
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

    override fun onDestroy() {
        super.onDestroy()
        playerHelper?.releasePlayer()
        playerHelper = null
        textExpandCollapseHelper.cleanup()
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