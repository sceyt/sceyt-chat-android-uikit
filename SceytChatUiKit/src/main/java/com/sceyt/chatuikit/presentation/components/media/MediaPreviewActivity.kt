package com.sceyt.chatuikit.presentation.components.media

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.components.forward.ForwardActivity
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaFilesViewHolderFactory
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItem
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItemType
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = MediaPreviewStyle.Builder(this, null).build()

        binding = SceytActivityMediaPreviewBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        binding.applyStyle()

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
            finish()
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
            it.setSubtitle(style.mediaDateFormatter.format(this, Date(item.data.attachment.createdAt)))
        }
    }

    override fun onMediaClick() {
        with(binding.toolbar) {
            isVisible = !isVisible
            toggleFullScreen(!isVisible)
        }
    }

    private fun toggleFullScreen(isFullScreen: Boolean) {
        if (isFullScreen) {
            WindowInsetsControllerCompat(window, binding.root).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            WindowInsetsControllerCompat(window, binding.root).apply {
                show(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    fun isVisibleToolbar() = binding.toolbar.isVisible

    private fun setOrUpdateMediaAdapter(data: List<MediaItem>) {
        val newData = if (reversed) data.reversed() else data
        if (mediaAdapter == null) {
            mediaAdapter = MediaAdapter(newData.toArrayList(),
                MediaFilesViewHolderFactory(this, style).also {
                    it.setNeedMediaDataCallback { infoData -> viewModel.needMediaInfo(infoData) }
                    it.setClickListener { onMediaClick() }
                })
            if (openedWithAttachment?.type == AttachmentTypeEnum.Video.value)
                mediaAdapter?.shouldPlayVideoPath = openedWithAttachment?.filePath

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
        } else mediaAdapter?.notifyUpdate(newData, binding.rvMedia)
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
            viewModel.loadPrevAttachments(channelId, attachmentId, true,
                mediaTypes, mediaAdapter?.itemCount ?: 1)
        }
    }

    private fun checkAndLoadNext() {
        if (viewModel.canLoadNext()) {
            val attachmentId = getRequestAttachmentId(false) ?: return
            viewModel.loadNextAttachments(channelId, attachmentId, true,
                mediaTypes, mediaAdapter?.itemCount ?: 1)
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
        ActionDialog(this) {
            when (it) {
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

    protected open fun share(item: MediaItem) {
        val fileTypeTitle = if (item.type == MediaItemType.Image) getString(R.string.sceyt_image) else getString(R.string.sceyt_video)
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
            Toast.makeText(this, getString(R.string.sceyt_media_cannot_save_to_gallery), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, getString(R.string.sceyt_media_cannot_save_to_gallery), Toast.LENGTH_SHORT).show()
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

        fun launch(context: Context, attachment: SceytAttachment, from: SceytUser?, channelId: Long, reversed: Boolean = false) {
            context.launchActivity<MediaPreviewActivity> {
                putExtra(KEY_ATTACHMENT, attachment)
                putExtra(KEY_USER, from)
                putExtra(KEY_CHANNEL_ID, channelId)
                putExtra(KEY_REVERSED, reversed)
            }
        }
    }
}
