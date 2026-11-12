package com.sceyt.chatuikit.presentation.components.media.adapter

import android.graphics.drawable.Drawable
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.mappers.getInfoFromMetadata
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.preview.MediaPreviewStyle
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
class MediaPreviewDownloadRetryTest {

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
    }

    @After
    fun tearDown() {
        AttachmentTransferStateStore.clear()
        stopKoin()
        SceytKoinApp.koinApp = null
    }

    @Test
    fun `failed image download shows the download icon and taps reach the listener`() {
        val clicked = mutableListOf<MediaItem>()
        val preview = previewHolder(MediaFilesViewHolderFactory.ItemType.Image, clicked)
        val item = mediaItem(TransferState.ErrorDownload, AttachmentTypeEnum.Image, MediaItemType.Image)

        preview.holder.bind(item)

        assertThat(preview.progress.isVisible).isTrue()
        assertThat(preview.progress.centerIcon()).isSameInstanceAs(preview.loaderStyle.downloadIcon)
        preview.progress.performClick()
        assertThat(clicked).containsExactly(item)
    }

    @Test
    fun `paused video download shows the download icon`() {
        val preview = previewHolder(MediaFilesViewHolderFactory.ItemType.Video, mutableListOf())

        preview.holder.bind(mediaItem(TransferState.PauseDownload, AttachmentTypeEnum.Video, MediaItemType.Video))

        assertThat(preview.progress.isVisible).isTrue()
        assertThat(preview.progress.centerIcon()).isSameInstanceAs(preview.loaderStyle.downloadIcon)
    }

    @Test
    fun `running download shows the cancel icon and taps reach the listener`() {
        val clicked = mutableListOf<MediaItem>()
        val preview = previewHolder(MediaFilesViewHolderFactory.ItemType.Image, clicked)
        val item = mediaItem(TransferState.ErrorDownload, AttachmentTypeEnum.Image, MediaItemType.Image)
        preview.holder.bind(item)

        preview.holder.updateState(item.attachment.toTransferData(TransferState.Downloading, 40f), false)

        assertThat(preview.progress.isVisible).isTrue()
        assertThat(preview.progress.centerIcon()).isSameInstanceAs(preview.loaderStyle.cancelIcon)
        preview.progress.performClick()
        assertThat(clicked).containsExactly(item)
    }

    private fun previewHolder(
        type: MediaFilesViewHolderFactory.ItemType,
        clicked: MutableList<MediaItem>,
    ): PreviewHolder {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.SceytPreviewThemeDark)
        val activity = controller.setup().get()
        val style = MediaPreviewStyle.Builder(activity, null).build()
        val factory = MediaFilesViewHolderFactory(activity, style).apply {
            setLoaderClickListener { clicked.add(it) }
        }
        val holder = factory.createViewHolder(FrameLayout(activity), type.ordinal)
        return PreviewHolder(holder, holder.itemView.findViewById(R.id.progress), style.mediaLoaderStyle)
    }

    private fun CircularProgressView.centerIcon(): Drawable? =
        CircularProgressView::class.java.getDeclaredField("centerIcon").run {
            isAccessible = true
            get(this@centerIcon) as Drawable?
        }

    private class PreviewHolder(
        val holder: BaseFileViewHolder<MediaItem>,
        val progress: CircularProgressView,
        val loaderStyle: MediaLoaderStyle,
    )

    private fun mediaItem(
        state: TransferState,
        attachmentType: AttachmentTypeEnum,
        type: MediaItemType,
    ): MediaItem {
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
        return MediaItem(
            data = AttachmentWithUserData(attachment, user = null),
            type = type,
            _dataFromJson = attachment.getInfoFromMetadata(),
            _thumbPath = null,
            _transferData = attachment.toTransferData(),
        )
    }
}
