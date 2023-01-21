package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.databinding.ActivityMediaBinding
import com.sceyt.sceytchatuikit.extensions.checkAndAskPermissions
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.extensions.getFileUriWithProvider
import com.sceyt.sceytchatuikit.extensions.getMimeType
import com.sceyt.sceytchatuikit.extensions.initPermissionLauncher
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.ActionDialog.Action.Forward
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.ActionDialog.Action.Save
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.ActionDialog.Action.Share
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import java.io.File


class MediaActivity : AppCompatActivity(), OnMediaClickCallback {
    lateinit var binding: ActivityMediaBinding

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var fileToSaveAfterPermission: MediaFile? = null
    override fun onStart() {
        super.onStart()
        requestPermissionLauncher = initPermissionLauncher { isGranted ->
            if (isGranted) {
                fileToSaveAfterPermission?.let { save(it) }
            } else {
                Toast.makeText(this, getString(R.string.sceyt_media_cannot_save_to_gallery), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        statusBarIconsColorWithBackground(true)
        window.navigationBarColor = getCompatColorByTheme(R.color.sceyt_color_status_bar, true)
        initView()

        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding.toolbar.applySystemWindowInsetsPadding(applyTop = true)
        binding.root.applySystemWindowInsetsPadding(applyBottom = true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun initView() {
        val attachments = intent.extras?.getParcelableArrayList<SceytAttachment>(SCEYT_ATTACHMENTS)
        if (attachments.isNullOrEmpty()) {
            finish()
            return
        }
        val mediaFiles = arrayListOf<MediaFile>()
        attachments.forEach {
            val filepath = it.filePath
            if (filepath != null) {
                val fileType = when (it.type) {
                    "image" -> FileType.Image
                    "video" -> FileType.Video
                    else -> null
                }
                if (fileType != null) {
                    val dateText = DateTimeUtil.getDateTimeString(it.createdAt)
                    mediaFiles.add(
                        MediaFile(
                            title = it.name,
                            path = filepath,
                            type = fileType,
                            dateString = dateText,
                        ))
                }
            }
        }

        loadMediaDetail(mediaFiles[0])
        binding.vpMedia.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
            ) {
            }

            override fun onPageSelected(position: Int) {
                val media = mediaFiles[position]
                loadMediaDetail(media)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }

        })

        binding.vpMedia.adapter = MediaAdapter(supportFragmentManager, mediaFiles)
        binding.toolbar.navigationShareIcon.setOnClickListener {
            showActionsDialog(mediaFiles[binding.vpMedia.currentItem])
        }

        binding.toolbar.navigationIcon.setOnClickListener {
            finish()
        }
    }

    fun loadMediaDetail(media: MediaFile) {
        binding.toolbar.setTitle(media.title)
        binding.toolbar.setDate(media.dateString)
    }

    override fun onMediaClick() {
        binding.toolbar.visibility =
                if (binding.toolbar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
//        toggleFullScreen(binding.toolbar.visibility == View.GONE)
    }

    private fun toggleFullScreen(isFullScreen: Boolean) {
        if (isFullScreen) {
            WindowInsetsControllerCompat(
                window,
                binding.root
            ).hide(WindowInsetsCompat.Type.systemBars())
        } else {
            WindowInsetsControllerCompat(
                window,
                binding.root
            ).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun isShowMediaDetail() = binding.toolbar.visibility == View.VISIBLE

    private fun showActionsDialog(file: MediaFile) {
        ActionDialog(this, file) {
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
        }.show()
    }

    private fun share(file: MediaFile) {
        val fileTypeTitle = if (file.type == FileType.Image) getString(R.string.sceyt_image) else getString(R.string.sceyt_video)
        val mimeType = getMimeTypeFrom(file)
        File(file.path).let {
            ShareCompat.IntentBuilder(this)
                .setStream(getFileUriWithProvider(it))
                .setType(mimeType)
                .setChooserTitle("${getString(R.string.sceyt_share)} $fileTypeTitle")
                .startChooser()
        }
    }

    private fun forward(file: MediaFile) {
        // To do
        Toast.makeText(this@MediaActivity, "Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun save(file: MediaFile) {
        val mimeType = getMimeTypeFrom(file)

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
        }
    }

    private fun getMimeTypeFrom(file: MediaFile): String {
        var mimeType = getMimeType(file.path)
        if (mimeType.isNullOrBlank()) mimeType = if (file.type == FileType.Image) "image/jpeg" else "video/mp4"
        return mimeType
    }

    companion object {
        private const val SCEYT_ATTACHMENTS = "sceyt_attachments"
        fun openMediaView(context: Context, attachment: SceytAttachment) {
            val items = java.util.ArrayList<SceytAttachment>().apply { add(attachment) }
            openMediaView(context, items)
        }

        fun openMediaView(context: Context, attachments: Array<SceytAttachment>) = openMediaView(
            context = context,
            attachments = java.util.ArrayList<SceytAttachment>().apply {
                attachments.forEach { add(it) }
            }
        )

        fun openMediaView(context: Context, attachments: java.util.ArrayList<SceytAttachment>) = launch(
            context = context,
            extras = bundleOf(
                SCEYT_ATTACHMENTS to attachments,
            ),
        )

        private fun launch(context: Context, extras: Bundle? = null) {
            context.launchActivity<MediaActivity> {
                extras?.let { putExtras(extras) }
            }
        }
    }
}
