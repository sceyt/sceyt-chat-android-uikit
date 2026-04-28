package com.sceyt.chatuikit.presentation.components.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.transition.ChangeBounds
import android.transition.ChangeClipBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.toIntentPayload
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytActivityMediaPreviewBinding
import com.sceyt.chatuikit.extensions.applySystemWindowInsetsPadding
import com.sceyt.chatuikit.extensions.checkAndAskPermissions
import com.sceyt.chatuikit.extensions.createIntent
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.getFileUriWithProvider
import com.sceyt.chatuikit.extensions.getFirstVisibleItemPosition
import com.sceyt.chatuikit.extensions.getMimeType
import com.sceyt.chatuikit.extensions.getPermissionsForMangeStorage
import com.sceyt.chatuikit.extensions.initPermissionLauncher
import com.sceyt.chatuikit.extensions.isFirstItemDisplaying
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.saveToGallery
import com.sceyt.chatuikit.extensions.transitionListener
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.navigate
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaFilesViewHolderFactory
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItem
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItemType
import com.sceyt.chatuikit.presentation.components.media.adapter.holders.SharedTransitionViewProvider
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog
import com.sceyt.chatuikit.presentation.components.media.viewmodel.MediaViewModel
import com.sceyt.chatuikit.presentation.components.media.viewmodel.MediaViewModelFactory
import com.sceyt.chatuikit.styles.preview.MediaPreviewStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Date

open class MediaPreviewActivity : AppCompatActivity(), OnMediaClickCallback {
    protected lateinit var binding: SceytActivityMediaPreviewBinding
    protected val viewModel by viewModels<MediaViewModel> {
        MediaViewModelFactory(intent)
    }
    protected lateinit var style: MediaPreviewStyle
    protected var fileToSaveAfterPermission: MediaItem? = null
    protected var mediaAdapter: MediaAdapter? = null
    protected var currentItem: MediaItem? = null
    protected var showInChatChannel: SceytChannel? = null
    protected var sharedTransitionStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (launchedWithSharedTransition()) {
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        }
        super.onCreate(savedInstanceState)
        style = MediaPreviewStyle.Builder(this, null).build()

        binding = SceytActivityMediaPreviewBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        binding.applyStyle()
        if (launchedWithSharedTransition()) {
            postponeEnterTransition()
            setupSharedElementTransition()
        }

        getDataFromIntent()
        initPageWithData()
        initViews()
        initViewModel()
    }

    override fun onPause() {
        super.onPause()
        mediaAdapter?.pauseAllVideos()
    }

    override fun onResume() {
        super.onResume()
        mediaAdapter?.resumeLastVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaAdapter?.releaseAllPlayers()
    }

    private fun getDataFromIntent() {
        showInChatChannel = intent?.extras?.parcelable(KEY_SHOW_IN_CHAT_CHANNEL)
    }

    private fun initViewModel() {
        viewModel.mediaItems.onEach { items ->
            if (mediaAdapter == null) {
                initMediaAdapter(items)
            } else {
                mediaAdapter?.submitList(items)
            }
        }.launchIn(lifecycleScope)
    }

    private fun initViews() {
        binding.toolbar.applySystemWindowInsetsPadding(
            applyTop = true,
            applyRight = true,
            applyLeft = true,
            applyBottom = false
        )

        binding.root.post { toggleFullScreen(false) }

        binding.toolbar.setNavigationClickListener {
            closeWithTransition()
        }

        binding.toolbar.setMenuClickListener { itemId ->
            if (itemId == R.id.sceyt_more) {
                currentItem?.let { showActionsDialog(it) }
            }
        }
    }

    private fun initPageWithData() {
        val items = viewModel.mediaItems.value
        val initialItem = items.getOrNull(viewModel.initialScrollIndex) ?: items.firstOrNull()
        initialItem?.let { loadMediaDetail(it) }
        startSharedTransitionWhenReady()
    }

    private fun loadMediaDetail(item: MediaItem) {
        currentItem = item
        val name = item.data.user?.let {
            style.userNameFormatter.format(this, it)
        }
        binding.toolbar.let {
            it.setTitle(name)
            it.setSubtitle(
                style.mediaDateFormatter.format(
                    context = this,
                    from = Date(item.data.attachment.createdAt)
                )
            )
        }
    }

    override fun onMediaClick() {
        with(binding.toolbar) {
            isVisible = !isVisible
            toggleFullScreen(!isVisible)
        }
    }

    private fun toggleFullScreen(isFullScreen: Boolean) {
        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isFullScreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    fun isVisibleToolbar() = binding.toolbar.isVisible

    private fun initMediaAdapter(data: List<MediaItem>) {
        mediaAdapter = MediaAdapter(
            attachmentViewHolderFactory = MediaFilesViewHolderFactory(this, style).also {
                it.setNeedMediaDataCallback { infoData -> viewModel.needMediaInfo(infoData) }
                it.setClickListener { onMediaClick() }
            },
            scope = lifecycleScope,
        ).also { adapter ->
            val attachment = viewModel.openedWithAttachment
            if (attachment?.type == AttachmentTypeEnum.Video.value)
                adapter.shouldPlayVideoPath = attachment.filePath
            adapter.submitList(data)
        }

        val scrollIndex = viewModel.initialScrollIndex
        if (scrollIndex > 0) binding.rvMedia.scrollToPosition(scrollIndex)

        binding.rvMedia.apply {
            adapter = mediaAdapter
            PagerSnapHelper().attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isLastItemDisplaying()) {
                        onLastItemDisplaying()
                    } else if (isFirstItemDisplaying())
                        onFirstItemDisplaying()
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val position = getFirstVisibleItemPosition()
                        mediaAdapter?.getData()?.getOrNull(position)?.let {
                            loadMediaDetail(it)
                        }
                        mediaAdapter?.shouldPlayVideoPath = null
                    }
                }
            })
        }
    }

    private fun closeWithTransition() {
        if (launchedWithSharedTransition()) {
            finishAfterTransition()
        } else {
            finish()
        }
    }

    private fun launchedWithSharedTransition(): Boolean {
        return intent.getBooleanExtra(EXTRA_SHARED_TRANSITION, false)
    }

    private fun startSharedTransitionWhenReady() {
        if (!launchedWithSharedTransition()) return
        binding.rvMedia.doOnPreDraw {
            lifecycleScope.launch {
                awaitSharedTransitionReady()
                startSharedTransitionIfNeeded()
            }
        }
    }

    private suspend fun awaitSharedTransitionReady() {
        val provider = getSharedTransitionViewProvider() ?: return
        withTimeoutOrNull(SHARED_TRANSITION_READY_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                provider.awaitReadyForSharedTransition {
                    continuation.safeResume(Unit)
                }
            }
        }
    }

    private fun setupSharedElementTransition() {
        window.sharedElementEnterTransition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeClipBounds())
            addTransition(ChangeImageTransform())
            duration = 220L
            addListener(
                transitionListener(onTransitionEnd = { refreshCurrentItem() })
            )
        }
        window.sharedElementReturnTransition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeClipBounds())
            addTransition(ChangeImageTransform())
            duration = 220L
        }
        window.enterTransition = Fade().apply {
            duration = 160L
            startDelay = 40L
        }
        window.returnTransition = Fade().apply {
            duration = 140L
        }
    }

    private fun findCurrentSharedElementView(): View? {
        return getSharedTransitionViewProvider()?.provide()
    }

    private fun getSharedTransitionViewProvider(): SharedTransitionViewProvider? {
        val position = binding.rvMedia.getFirstVisibleItemPosition()
        if (position == RecyclerView.NO_POSITION) return null
        return binding.rvMedia.findViewHolderForAdapterPosition(position) as? SharedTransitionViewProvider
    }

    private fun refreshCurrentItem() {
        val position = binding.rvMedia.getFirstVisibleItemPosition()
        if (position != RecyclerView.NO_POSITION) mediaAdapter?.notifyItemChanged(position, Unit)
    }

    private fun startSharedTransitionIfNeeded() {
        if (!launchedWithSharedTransition() || sharedTransitionStarted) return
        findCurrentSharedElementView()?.let {
            ViewCompat.setTransitionName(it, SHARED_TRANSITION_NAME)
        }
        sharedTransitionStarted = true
        startPostponedEnterTransition()
    }

    private fun onFirstItemDisplaying() {
        if (viewModel.reversed) {
            checkAndLoadNext()
        } else
            checkAndLoadPrev()
    }

    private fun onLastItemDisplaying() {
        if (viewModel.reversed) {
            checkAndLoadPrev()
        } else
            checkAndLoadNext()
    }

    private fun checkAndLoadPrev() = viewModel.checkAndLoadPrev()

    private fun checkAndLoadNext() = viewModel.checkAndLoadNext()

    protected open fun showActionsDialog(file: MediaItem) {
        ActionDialog(this, showInChatVisible = showInChatChannel != null) {
            when (it) {
                ActionDialog.Action.ShowInChat -> showInChat(file)
                ActionDialog.Action.Save -> {
                    fileToSaveAfterPermission = file
                    val permissions = getPermissionsForMangeStorage()
                    if (checkAndAskPermissions(requestPermissionLauncher, *permissions))
                        save(file)
                }

                ActionDialog.Action.Share -> share(file)
                ActionDialog.Action.Forward -> forward(file)
            }
        }.show()
    }

    protected open fun showInChat(item: MediaItem) {
        val channel = showInChatChannel ?: return
        SceytChatUIKit.navigator.navigate(
            context = this,
            destination = Destination.Channel(channel, item.data.attachment.messageId)
        )
        finish()
    }

    protected open fun share(item: MediaItem) {
        val fileTypeTitle = if (item.type == MediaItemType.Image)
            getString(R.string.sceyt_image) else getString(R.string.sceyt_video)
        item.attachment.filePath?.let { path ->
            File(path).let {
                val mimeType = getMimeTypeFrom(item.attachment)
                ShareCompat.IntentBuilder(this)
                    .setStream(getFileUriWithProvider(it))
                    .setType(mimeType)
                    .setChooserTitle("${getString(R.string.sceyt_share)} $fileTypeTitle")
                    .startChooser()
            }
        }
    }

    protected open fun forward(item: MediaItem) {
        lifecycleScope.launch {
            viewModel.getMessageById(item.data.attachment.messageId)?.let { message ->
                SceytChatUIKit.navigator.navigate(
                    context = this@MediaPreviewActivity,
                    destination = Destination.Forward(message)
                )
            } ?: run {
                customToastSnackBar("Couldn't forward this message")
            }
        }
    }

    protected open fun save(item: MediaItem) {
        val file = item.attachment
        val mimeType = getMimeTypeFrom(file)

        saveToGallery(
            context = this,
            path = file.filePath.toString(),
            name = file.name,
            mimeType = mimeType,
        )?.let {
            Toast.makeText(this, getString(R.string.sceyt_saved), Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(
                this, getString(R.string.sceyt_media_cannot_save_to_gallery), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getMimeTypeFrom(file: SceytAttachment): String {
        var mimeType = getMimeType(file.filePath)
        if (mimeType.isNullOrBlank())
            mimeType = if (file.type == AttachmentTypeEnum.Image.value)
                "image/jpeg" else "video/mp4"
        return mimeType
    }

    private val requestPermissionLauncher = initPermissionLauncher { isGranted ->
        if (isGranted) {
            fileToSaveAfterPermission?.let { save(it) }
        } else {
            Toast.makeText(
                this, getString(R.string.sceyt_media_cannot_save_to_gallery), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun SceytActivityMediaPreviewBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(toolbar)
    }

    companion object {
        private const val SHARED_TRANSITION_READY_TIMEOUT_MS = 300L
        private const val PREVIEW_LAUNCH_THROTTLE_MS = 300L
        private const val KEY_CHANNEL_ID = "KEY_CHANNEL_ID"
        private const val KEY_SHOW_IN_CHAT_CHANNEL = "KEY_SHOW_IN_CHAT_CHANNEL"
        private const val EXTRA_SHARED_TRANSITION = "EXTRA_SHARED_TRANSITION"
        const val SHARED_TRANSITION_NAME = "sceyt_media_preview"

        fun createIntent(
            context: Context,
            attachment: SceytAttachment,
            from: SceytUser?,
            channelId: Long,
            reversed: Boolean = false,
            showInChatChannel: SceytChannel? = null,
            launchedWithSharedTransition: Boolean = false,
        ): Intent = context.createIntent<MediaPreviewActivity> {
            fillLaunchIntent(
                attachment = attachment,
                from = from,
                channelId = channelId,
                reversed = reversed,
                showInChatChannel = showInChatChannel,
                launchedWithSharedTransition = launchedWithSharedTransition,
            )
        }

        fun createPreloadedIntent(
            context: Context,
            items: List<AttachmentWithUserData>,
            initialIndex: Int,
            showInChatChannel: SceytChannel? = null,
            launchedWithSharedTransition: Boolean = false,
        ): Intent {
            MediaPreviewTransferHolder.set(
                MediaPreviewTransferHolder.PreloadedData(items, initialIndex)
            )
            return context.createIntent<MediaPreviewActivity> {
                putExtra(EXTRA_SHARED_TRANSITION, launchedWithSharedTransition)
                showInChatChannel?.let {
                    putExtra(KEY_SHOW_IN_CHAT_CHANNEL, it.toIntentPayload())
                }
            }
        }

        private fun Intent.fillLaunchIntent(
            attachment: SceytAttachment,
            from: SceytUser?,
            channelId: Long,
            reversed: Boolean,
            showInChatChannel: SceytChannel?,
            launchedWithSharedTransition: Boolean,
        ) {
            putExtra(MediaViewModelFactory.KEY_ATTACHMENT, attachment)
            putExtra(MediaViewModelFactory.KEY_USER, from)
            putExtra(KEY_CHANNEL_ID, channelId)
            putExtra(MediaViewModelFactory.KEY_REVERSED, reversed)
            showInChatChannel?.let {
                putExtra(KEY_SHOW_IN_CHAT_CHANNEL, it.toIntentPayload())
            }
            if (launchedWithSharedTransition) {
                putExtra(EXTRA_SHARED_TRANSITION, true)
            }
        }

        @Synchronized
        fun canLaunchPreview(): Boolean {
            val now = SystemClock.elapsedRealtime()
            if (now - lastLaunchPreviewAtMs < PREVIEW_LAUNCH_THROTTLE_MS) return false
            lastLaunchPreviewAtMs = now
            return true
        }

        private var lastLaunchPreviewAtMs = 0L
    }
}
