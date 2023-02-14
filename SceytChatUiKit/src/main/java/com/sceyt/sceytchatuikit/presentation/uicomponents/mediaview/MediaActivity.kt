package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.databinding.ActivityMediaBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferUpdateObserver
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaFilesViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.viewmodel.MediaViewModel
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File


class MediaActivity : AppCompatActivity(), OnMediaClickCallback {
    lateinit var binding: ActivityMediaBinding
    private val viewModel by viewModels<MediaViewModel>()
    private var fileToSaveAfterPermission: MediaFile? = null
    private var channelId: Long = 0L
    private val mediaTypes = listOf(AttachmentTypeEnum.Image.value(), AttachmentTypeEnum.Video.value())
    private var mediaAdapter: MediaAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        getDataFromIntent()
        initPageWithData()
        initViews()
        initViewModel()
    }

    private fun getDataFromIntent() {
        channelId = intent.getLongExtra(SCEYT_CHANNEL_ID, 0L)
    }

    private fun initViewModel() {
        viewModel.fileFilesFlow.onEach {
            when (it) {
                is PaginationResponse.DBResponse -> {
                    val data = viewModel.mapToMediaItem(it.data)

                    when (it.loadType) {
                        LoadPrev -> mediaAdapter?.addPrevItems(data)
                        LoadNext -> mediaAdapter?.addNextItems(data)
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

        MessageEventsObserver.onTransferUpdatedFlow
            .onEach {
                TransferUpdateObserver.update(it)
            }
            .launchIn(lifecycleScope)
    }

    private fun initPageWithData() {
        val attachment = intent?.extras?.getParcelable<SceytAttachment>(SCEYT_ATTACHMENTS)
        val user = intent?.extras?.getSerializable(SCEYT_USER) as User?

        if (attachment == null) {
            viewModel.loadPrevAttachments(channelId, 0, false, mediaTypes, 0)
            return
        } else {
            val mediaFiles = arrayListOf<MediaItem>()

            val mediaItem = when (attachment.type) {
                AttachmentTypeEnum.Image.value() -> MediaItem.Image(AttachmentWithUserData(attachment, user))
                AttachmentTypeEnum.Video.value() -> MediaItem.Video(AttachmentWithUserData(attachment, user))
                else -> null
            }
            if (mediaItem != null) {
                mediaFiles.add(mediaItem)
                loadMediaDetail(mediaItem.data.attachment, mediaItem.data.user)
            }

            setOrUpdateMediaAdapter(mediaFiles)

            binding.root.post {
                if (attachment.id == null || attachment.id == 0L)
                    viewModel.loadPrevAttachments(channelId, 0, false, mediaTypes, 0)
                else
                    viewModel.loadNearPrevAttachments(channelId, attachment.id, mediaTypes, 0)
            }
        }


        /*   binding.vpMedia.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
               override fun onPageSelected(position: Int) {
                   mediaAdapter?.getData()?.getOrNull(position)?.getData()?.let {
                       loadMediaDetail(it.attachment, it.user)
                   }
               }
           })
   */
    }

    private fun initViews() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.toolbar.applySystemWindowInsetsPadding(applyTop = true)

        binding.root.post { toggleFullScreen(false) }

        binding.toolbar.navigationShareIcon.setOnClickListener {
            //showActionsDialog(mediaFiles[binding.vpMedia.currentItem])
        }

        binding.toolbar.navigationIcon.setOnClickListener {
            finish()
        }
    }

    fun loadMediaDetail(media: SceytAttachment, user: User?) {
        binding.toolbar.setTitle(user?.getPresentableName() ?: "")
        binding.toolbar.setDate(DateTimeUtil.getDateTimeString(media.createdAt, "MM.dd.yy, HH:mm"))
    }

    override fun onMediaClick() {
        binding.toolbar.visibility =
                if (binding.toolbar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        toggleFullScreen(binding.toolbar.visibility == View.GONE)
    }

    private fun toggleFullScreen(isFullScreen: Boolean) {
        if (isFullScreen) {
            WindowInsetsControllerCompat(
                window,
                binding.root,
            ).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            WindowInsetsControllerCompat(
                window,
                binding.root,
            ).apply {
                show(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    fun isShowMediaDetail() = binding.toolbar.visibility == View.VISIBLE

    private fun setOrUpdateMediaAdapter(data: List<MediaItem>) {
        if (mediaAdapter == null) {
            val snapHelper: SnapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(binding.vpMedia)

            mediaAdapter = MediaAdapter(data.toArrayList(), MediaFilesViewHolderFactory(this).also {
                it.setNeedMediaDataCallback { infoData -> viewModel.needMediaInfo(infoData) }

                it.setClickListener { onMediaClick() }
            })

            binding.vpMedia.apply {
                adapter = mediaAdapter

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (isLastItemDisplaying()) {
                            if (viewModel.canLoadNext()) {
                                viewModel.loadNextAttachments(channelId, mediaAdapter?.getLastMediaItem()?.data?.attachment?.id
                                        ?: return, true,
                                    mediaTypes, adapter?.itemCount ?: 1)
                            }
                        } else if (isFirstItemDisplaying()) {
                            if (viewModel.canLoadPrev()) {
                                viewModel.loadPrevAttachments(channelId, mediaAdapter?.getFirstMediaItem()?.data?.attachment?.id
                                        ?: return, true,
                                    mediaTypes, adapter?.itemCount ?: 1)
                            }
                        }
                    }

                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                    }
                })
            }
        } else mediaAdapter?.notifyUpdate(data, binding.vpMedia)
    }

    private fun showActionsDialog(file: MediaItem) {
        /* ActionDialog(this, file) {
             when (it) {
                 Save -> {
                     fileToSaveAfterPermission = file
                     if (checkAndAskPermissions(requestPermissionLauncher, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                         save(file)
                     }
                 }
                 Share -> share(file)
                 Forward -> forward(file)
             }
         }.show()*/
    }

    private fun share(item: MediaItem) {
        val fileTypeTitle = if (item is MediaItem.Image) getString(R.string.sceyt_image) else getString(R.string.sceyt_video)
        item.file.filePath?.let { path ->
            File(path).let {
                val mimeType = getMimeTypeFrom(item.file)
                ShareCompat.IntentBuilder(this)
                    .setStream(getFileUriWithProvider(it))
                    .setType(mimeType)
                    .setChooserTitle("${getString(R.string.sceyt_share)} $fileTypeTitle")
                    .startChooser()
            }
        }
    }

    private fun forward(file: MediaFile) {
        // To do
        Toast.makeText(this@MediaActivity, "Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun save(file: MediaFile) {
        /* val mimeType = getMimeTypeFrom(file)

         var extension = File(file.path).extension
         if (extension.isBlank()) extension = if (file.type == FileType.Image) "jpg" else "mp4"

         saveToGallery(
             context = this,
             path = file.path,
             name = "${file.title}.$extension",
             mimeType = mimeType,
         )?.let {
             Toast.makeText(this, getString(R.string.sceyt_saved), Toast.LENGTH_LONG).show()
         } ?: run {
             Toast.makeText(this, getString(R.string.sceyt_media_cannot_save_to_gallery), Toast.LENGTH_LONG).show()
         }*/
    }

    private fun getMimeTypeFrom(file: SceytAttachment): String {
        var mimeType = getMimeType(file.filePath)
        if (mimeType.isNullOrBlank()) mimeType = if (file.type == AttachmentTypeEnum.Image.value()) "image/jpeg" else "video/mp4"
        return mimeType
    }

    private val requestPermissionLauncher = initPermissionLauncher { isGranted ->
        if (isGranted) {
            fileToSaveAfterPermission?.let { save(it) }
        } else {
            Toast.makeText(this, getString(R.string.sceyt_media_cannot_save_to_gallery), Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val SCEYT_ATTACHMENTS = "sceyt_attachments"
        private const val SCEYT_USER = "SCEYT_USER"
        private const val SCEYT_CHANNEL_ID = "SCEYT_CHANNEL_ID"

        fun openMediaView(context: Context, attachment: SceytAttachment, from: User?, channelId: Long) {
            context.launchActivity<MediaActivity> {
                putExtra(SCEYT_ATTACHMENTS, attachment)
                putExtra(SCEYT_USER, from)
                putExtra(SCEYT_CHANNEL_ID, channelId)
            }
        }
    }
}
