package com.sceyt.chatuikit.presentation.components.media

import android.app.Activity
import android.app.SharedElementCallback
import android.content.Context
import android.os.Bundle
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
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.ShareCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.toIntentPayload
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytActivityMediaPreviewBinding
import com.sceyt.chatuikit.extensions.applySystemWindowInsetsPadding
import com.sceyt.chatuikit.extensions.checkAndAskPermissions
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.getFileUriWithProvider
import com.sceyt.chatuikit.extensions.getFirstVisibleItemPosition
import com.sceyt.chatuikit.extensions.getMimeType
import com.sceyt.chatuikit.extensions.getPermissionsForMangeStorage
import com.sceyt.chatuikit.extensions.initPermissionLauncher
import com.sceyt.chatuikit.extensions.isFirstItemDisplaying
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.saveToGallery
import com.sceyt.chatuikit.extensions.transitionListener
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.forward.ForwardActivity
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaFilesViewHolderFactory
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItem
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItemType
import com.sceyt.chatuikit.presentation.components.media.adapter.holders.SharedTransitionViewProvider
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog
import com.sceyt.chatuikit.presentation.components.media.viewmodel.MediaViewModel
import com.sceyt.chatuikit.styles.preview.MediaPreviewStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.util.Date

open class MediaPreviewActivity : AppCompatActivity(), OnMediaClickCallback {
    lateinit var binding: SceytActivityMediaPreviewBinding
    private val viewModel by viewModels<MediaViewModel>()
    protected lateinit var style: MediaPreviewStyle
    private var fileToSaveAfterPermission: MediaItem? = null
    private var channelId: Long = 0L
    private val mediaTypes = listOf(AttachmentTypeEnum.Image.value, AttachmentTypeEnum.Video.value)
    private var mediaAdapter: MediaAdapter? = null
    private var currentItem: MediaItem? = null
    private var openedWithAttachment: SceytAttachment? = null
    private var reversed = false
    private var showInChatChannel: SceytChannel? = null
    private var sharedTransitionStarted = false

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
            setupExitSharedElementCallback()
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
        channelId = intent.getLongExtra(KEY_CHANNEL_ID, 0L)
        reversed = intent.getBooleanExtra(KEY_REVERSED, false)
        showInChatChannel = intent?.extras?.parcelable(KEY_SHOW_IN_CHAT_CHANNEL)
    }

    private fun initViewModel() {
        viewModel.fileFilesFlow.onEach {
            when (it) {
                is PaginationResponse.DBResponse -> {
                    val data = viewModel.mapToMediaItem(it.data)

                    when (it.loadType) {
                        LoadPrev -> {
                            if (reversed) {
                                mediaAdapter?.addNextItems(data.reversed())
                            } else mediaAdapter?.addPrevItems(data)
                        }

                        LoadNext -> {
                            if (reversed) {
                                mediaAdapter?.addPrevItems(data.reversed())
                            } else mediaAdapter?.addNextItems(data)
                        }

                        LoadNear -> setOrUpdateMediaAdapter(data)
                        else -> return@onEach
                    }
                }

                is PaginationResponse.ServerResponse -> {
                    if (it.hasDiff) {
                        val data = viewModel.mapToMediaItem(it.cacheData)
                        setOrUpdateMediaAdapter(data)
                    }
                }

                else -> return@onEach
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
        val attachment = intent?.extras?.parcelable<SceytAttachment>(KEY_ATTACHMENT).also {
            openedWithAttachment = it
        }
        val user = intent?.extras?.parcelable<SceytUser>(KEY_USER)

        if (attachment == null) {
            viewModel.loadPrevAttachments(channelId, 0, false, mediaTypes, 0)
            return
        } else {
            val mediaFiles = arrayListOf<MediaItem>()
            val mediaItem = viewModel.toMediaItem(AttachmentWithUserData(attachment, user))
            if (mediaItem != null) {
                mediaFiles.add(mediaItem)
                loadMediaDetail(mediaItem)
            }

            setOrUpdateMediaAdapter(mediaFiles)

            binding.root.post {
                if (attachment.id == null || attachment.id == 0L)
                    viewModel.loadPrevAttachments(channelId, 0, false, mediaTypes, 0)
                else
                    viewModel.loadNearAttachments(channelId, attachment.id, mediaTypes, 0)
            }
        }
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

    private fun setOrUpdateMediaAdapter(data: List<MediaItem>) {
        val newData = if (reversed) data.reversed() else data
        if (mediaAdapter == null) {
            mediaAdapter = MediaAdapter(
                attachments = newData.toArrayList(),
                attachmentViewHolderFactory = MediaFilesViewHolderFactory(this, style).also {
                    it.setNeedMediaDataCallback { infoData -> viewModel.needMediaInfo(infoData) }
                    it.setClickListener { onMediaClick() }
                })
            if (openedWithAttachment?.type == AttachmentTypeEnum.Video.value)
                mediaAdapter?.shouldPlayVideoPath = openedWithAttachment?.filePath

            binding.rvMedia.apply {
                adapter = mediaAdapter
                PagerSnapHelper().attachToRecyclerView(this)
                if (launchedWithSharedTransition()) {
                    doOnPreDraw { startSharedTransitionIfNeeded() }
                }

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
        } else mediaAdapter?.notifyUpdate(newData, binding.rvMedia)
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

    private fun setupExitSharedElementCallback() {
        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>,
                sharedElements: MutableMap<String, View>,
            ) {
                val sharedElement = findCurrentSharedElementView()
                if (sharedElement == null) {
                    names.clear()
                    sharedElements.clear()
                    return
                }
                ViewCompat.setTransitionName(sharedElement, SHARED_TRANSITION_NAME)
                names.clear()
                names.add(SHARED_TRANSITION_NAME)
                sharedElements.clear()
                sharedElements[SHARED_TRANSITION_NAME] = sharedElement
            }
        })
    }

    private fun findCurrentSharedElementView(): View? {
        val position = binding.rvMedia.getFirstVisibleItemPosition()
        if (position == RecyclerView.NO_POSITION) return null
        val viewHolder =
            binding.rvMedia.findViewHolderForAdapterPosition(position) as? SharedTransitionViewProvider
        return viewHolder?.provide()
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
        if (reversed) {
            checkAndLoadNext()
        } else
            checkAndLoadPrev()
    }

    private fun onLastItemDisplaying() {
        if (reversed) {
            checkAndLoadPrev()
        } else
            checkAndLoadNext()
    }

    private fun checkAndLoadPrev() {
        if (viewModel.canLoadPrev()) {
            val attachmentId = getRequestAttachmentId(true) ?: return
            viewModel.loadPrevAttachments(
                channelId = channelId,
                lastAttachmentId = attachmentId,
                isLoadingMore = true,
                type = mediaTypes,
                offset = mediaAdapter?.itemCount ?: 1
            )
        }
    }

    private fun checkAndLoadNext() {
        if (viewModel.canLoadNext()) {
            val attachmentId = getRequestAttachmentId(false) ?: return
            viewModel.loadNextAttachments(
                channelId = channelId,
                lastAttachmentId = attachmentId,
                isLoadingMore = true,
                type = mediaTypes,
                offset = mediaAdapter?.itemCount ?: 1
            )
        }
    }

    private fun getRequestAttachmentId(loadPrev: Boolean): Long? {
        mediaAdapter?.let { adapter ->
            val attachmentId = if (loadPrev) {
                if (reversed)
                    adapter.getLastMediaItem().data.attachment.id
                else adapter.getFirstMediaItem().data.attachment.id
            } else {
                if (reversed)
                    adapter.getFirstMediaItem().data.attachment.id
                else adapter.getLastMediaItem().data.attachment.id
            }

            return attachmentId
        }
        return null
    }

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
        ChannelActivity.launch(this, channel, item.data.attachment.messageId)
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
        viewModel.getMessageById(item.data.attachment.messageId).onEach {
            it?.let { message ->
                ForwardActivity.launch(this, message)
            } ?: run {
                customToastSnackBar("Couldn't forward this message")
            }
        }.launchIn(viewModel.viewModelScope)
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
        private const val KEY_ATTACHMENT = "KEY_ATTACHMENT"
        private const val KEY_USER = "KEY_USER"
        private const val KEY_CHANNEL_ID = "KEY_CHANNEL_ID"
        private const val KEY_REVERSED = "KEY_REVERSED"
        private const val KEY_SHOW_IN_CHAT_CHANNEL = "KEY_SHOW_IN_CHAT_CHANNEL"
        private const val EXTRA_SHARED_TRANSITION = "EXTRA_SHARED_TRANSITION"
        const val SHARED_TRANSITION_NAME = "sceyt_media_preview"

        fun launch(
            context: Context,
            attachment: SceytAttachment,
            from: SceytUser?,
            channelId: Long,
            reversed: Boolean = false,
            showInChatChannel: SceytChannel? = null,
        ) {
            context.launchActivity<MediaPreviewActivity> {
                fillLaunchIntent(
                    attachment = attachment,
                    from = from,
                    channelId = channelId,
                    reversed = reversed,
                    showInChatChannel = showInChatChannel,
                    launchedWithSharedTransition = false,
                )
            }
        }

        fun launch(
            activity: Activity,
            attachment: SceytAttachment,
            from: SceytUser?,
            channelId: Long,
            reversed: Boolean = false,
            showInChatChannel: SceytChannel? = null,
            sourceView: View
        ) {
            ViewCompat.setTransitionName(sourceView, SHARED_TRANSITION_NAME)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                sourceView,
                SHARED_TRANSITION_NAME
            )
            activity.launchActivity<MediaPreviewActivity>(
                options = options.toBundle() ?: Bundle()
            ) {
                fillLaunchIntent(
                    attachment = attachment,
                    from = from,
                    channelId = channelId,
                    reversed = reversed,
                    showInChatChannel = showInChatChannel,
                    launchedWithSharedTransition = true,
                )
            }
        }

        private fun android.content.Intent.fillLaunchIntent(
            attachment: SceytAttachment,
            from: SceytUser?,
            channelId: Long,
            reversed: Boolean,
            showInChatChannel: SceytChannel?,
            launchedWithSharedTransition: Boolean,
        ) {
            putExtra(KEY_ATTACHMENT, attachment)
            putExtra(KEY_USER, from)
            putExtra(KEY_CHANNEL_ID, channelId)
            putExtra(KEY_REVERSED, reversed)
            showInChatChannel?.let {
                putExtra(KEY_SHOW_IN_CHAT_CHANNEL, it.toIntentPayload())
            }
            if (launchedWithSharedTransition) {
                putExtra(EXTRA_SHARED_TRANSITION, true)
            }
        }
    }
}
