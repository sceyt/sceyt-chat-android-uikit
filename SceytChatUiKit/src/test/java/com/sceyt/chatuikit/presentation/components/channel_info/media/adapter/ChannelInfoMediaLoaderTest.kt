package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.databinding.SceytItemChannelImageBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelVideoBinding
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.mappers.getInfoFromMetadata
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItemType
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders.ImageViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders.VideoViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.channel_info.media.ChannelInfoMediaItemStyle
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChannelInfoMediaLoaderTest {

    private val clicked = mutableListOf<ChannelFileItem>()
    private val clickListeners = AttachmentClickListenersImpl().apply {
        setListener(AttachmentClickListeners.AttachmentLoaderClickListener { _, item -> clicked.add(item) })
    }
    private lateinit var activity: AppCompatActivity
    private lateinit var style: ChannelInfoMediaItemStyle

    @Before
    fun setUp() {
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) {
                    Dispatchers.Unconfined
                }
            })
        }
        AttachmentTransferStateStore.clear()
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.SceytPreviewThemeDark)
        activity = controller.setup().get()
        style = ChannelInfoMediaItemStyle.Builder(activity, null).build()
    }

    @After
    fun tearDown() {
        AttachmentTransferStateStore.clear()
        stopKoin()
        SceytKoinApp.koinApp = null
    }

    @Test
    fun `failed image download shows the download icon and taps reach the loader listener`() {
        val binding = SceytItemChannelImageBinding.inflate(LayoutInflater.from(activity), FrameLayout(activity), false)
        val holder = ImageViewHolder(binding, style, clickListeners, needMediaDataCallback = {})
        val item = fileItem(TransferState.ErrorDownload, AttachmentTypeEnum.Image, ChannelFileItemType.Image)

        holder.bind(item)

        assertThat(binding.loadProgress.isVisible).isTrue()
        assertThat(binding.loadProgress.centerIcon()).isSameInstanceAs(style.mediaLoaderStyle.downloadIcon)
        binding.loadProgress.performClick()
        assertThat(clicked).containsExactly(item)
    }

    @Test
    fun `running video download shows the cancel icon`() {
        val binding = SceytItemChannelVideoBinding.inflate(LayoutInflater.from(activity), FrameLayout(activity), false)
        val holder = VideoViewHolder(binding, style, clickListeners, needMediaDataCallback = {})

        holder.bind(fileItem(TransferState.Downloading, AttachmentTypeEnum.Video, ChannelFileItemType.Video))

        assertThat(binding.loadProgress.isVisible).isTrue()
        assertThat(binding.loadProgress.centerIcon()).isSameInstanceAs(style.mediaLoaderStyle.cancelIcon)
    }

    @Test
    fun `failed upload keeps the upload icon after its thumbnail loads`() {
        val binding = SceytItemChannelImageBinding.inflate(LayoutInflater.from(activity), FrameLayout(activity), false)
        val holder = ImageViewHolder(binding, style, clickListeners, needMediaDataCallback = {})
        val item = fileItem(TransferState.ErrorUpload, AttachmentTypeEnum.Image, ChannelFileItemType.Image)
        holder.bind(item)

        holder.updateState(item.attachment.toTransferData(TransferState.ThumbLoaded), false)

        assertThat(binding.loadProgress.isVisible).isTrue()
        assertThat(binding.loadProgress.centerIcon()).isSameInstanceAs(style.mediaLoaderStyle.uploadIcon)
    }

    @Test
    fun `downloaded image hides the loader`() {
        val binding = SceytItemChannelImageBinding.inflate(LayoutInflater.from(activity), FrameLayout(activity), false)
        val holder = ImageViewHolder(binding, style, clickListeners, needMediaDataCallback = {})

        holder.bind(fileItem(TransferState.Downloaded, AttachmentTypeEnum.Image, ChannelFileItemType.Image))

        assertThat(binding.loadProgress.isVisible).isFalse()
    }

    private fun fileItem(
        state: TransferState,
        attachmentType: AttachmentTypeEnum,
        type: ChannelFileItemType,
    ): ChannelFileItem.Item {
        val attachment = SceytAttachment(
            id = 1L,
            messageId = 1L,
            messageTid = 11L,
            userId = null,
            name = "media",
            type = attachmentType.value,
            metadata = null,
            fileSize = 0L,
            createdAt = 1000L,
            url = "https://cdn.example/1",
            filePath = null,
            transferState = state,
            progressPercent = 0f,
            originalFilePath = null,
            linkPreviewDetails = null,
        )
        return ChannelFileItem.Item(
            data = AttachmentWithUserData(attachment, user = null),
            type = type,
            _metadataPayload = attachment.getInfoFromMetadata(),
            _thumbPath = null,
            _transferData = attachment.toTransferData(),
        )
    }

    private fun CircularProgressView.centerIcon(): Drawable? =
        CircularProgressView::class.java.getDeclaredField("centerIcon").run {
            isAccessible = true
            get(this@centerIcon) as Drawable?
        }
}
