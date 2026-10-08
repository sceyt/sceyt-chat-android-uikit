package com.sceyt.chatuikit.presentation.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.Request
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.isFileNotFound
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AttachmentViewHolderHelperGlideAndroidTest {
    private lateinit var context: Context
    private lateinit var dir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dir = File(context.cacheDir, "glide_thumb_test").apply { mkdirs() }
        clearGlide()
    }

    @After
    fun tearDown() {
        clearGlide()
        dir.deleteRecursively()
    }

    @Test
    fun glideReportsMissingFileAsFileNotFound() {
        val error = loadError(File(dir, "missing.jpg").path)

        assertThat(error.isFileNotFound()).isTrue()
    }

    @Test
    fun glideReportsCorruptFileAsOtherError() {
        val corrupt = File(dir, "corrupt.jpg").apply { writeBytes(ByteArray(512) { it.toByte() }) }

        val error = loadError(corrupt.path)

        assertThat(error.isFileNotFound()).isFalse()
    }

    @Test
    fun missingThumbFileRequestsNewThumbOnce() {
        val item = TestAttachmentItem(attachment(), File(dir, "deleted.jpg").path)
        val requested = CountDownLatch(1)
        val requestCount = AtomicInteger()

        drawThumbOrRequest(item) {
            requestCount.incrementAndGet()
            requested.countDown()
        }

        assertThat(requested.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(item.thumbPath).isNull()
        assertThat(requestCount.get()).isEqualTo(1)
    }

    @Test
    fun corruptThumbFileDoesNotRequestThumb() {
        val corrupt = File(dir, "corrupt.jpg").apply { writeBytes(ByteArray(512) { it.toByte() }) }
        val item = TestAttachmentItem(attachment(), corrupt.path)
        val requestCount = AtomicInteger()

        val imageView = drawThumbOrRequest(item) { requestCount.incrementAndGet() }

        assertThat(awaitGlideRequestDone(imageView)).isTrue()
        assertThat(requestCount.get()).isEqualTo(0)
        assertThat(item.thumbPath).isEqualTo(corrupt.path)
    }

    @Test
    fun existingThumbDrawsWithoutRequest() {
        val thumb = writeJpeg(File(dir, "thumb.jpg"))
        val item = TestAttachmentItem(attachment(), thumb.path)
        val requestCount = AtomicInteger()

        val imageView = drawThumbOrRequest(item) { requestCount.incrementAndGet() }

        assertThat(awaitDrawable(imageView)).isNotNull()
        assertThat(requestCount.get()).isEqualTo(0)
        assertThat(item.thumbPath).isEqualTo(thumb.path)
    }

    private fun drawThumbOrRequest(item: TestAttachmentItem, requestThumb: () -> Unit): ImageView {
        lateinit var imageView: ImageView
        onMain {
            imageView = ImageView(context)
            val helper = AttachmentViewHolderHelper(View(context))
            helper.bind(item, Size(100, 100))
            helper.drawThumbOrRequest(imageView, requestThumb)
        }
        return imageView
    }

    private fun awaitDrawable(imageView: ImageView): Drawable? {
        repeat(50) {
            var drawable: Drawable? = null
            onMain { drawable = imageView.drawable }
            if (drawable != null) return drawable
            Thread.sleep(100)
        }
        return null
    }

    private fun awaitGlideRequestDone(imageView: ImageView): Boolean {
        repeat(50) {
            var done = false
            onMain {
                val request = imageView.getTag(com.bumptech.glide.R.id.glide_custom_view_target_tag) as? Request
                done = request != null && !request.isRunning
            }
            if (done) return true
            Thread.sleep(100)
        }
        return false
    }

    private fun loadError(path: String): GlideException? {
        val future = Glide.with(context).load(path).submit(100, 100)
        return try {
            future.get(5, TimeUnit.SECONDS)
            null
        } catch (e: ExecutionException) {
            e.cause as? GlideException
        }
    }

    private fun writeJpeg(file: File): File {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file
    }

    private fun clearGlide() {
        onMain { Glide.get(context).clearMemory() }
        Glide.get(context).clearDiskCache()
    }

    private fun onMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    private fun attachment() = SceytAttachment(
        id = MESSAGE_TID,
        messageId = MESSAGE_TID,
        messageTid = MESSAGE_TID,
        userId = null,
        name = "image.jpg",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 100L,
        createdAt = 1_000L,
        url = "https://cdn.test/image.jpg",
        filePath = "/downloads/image.jpg",
        transferState = TransferState.Downloaded,
        progressPercent = 100f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private class TestAttachmentItem(
        private var currentAttachment: SceytAttachment,
        @Volatile private var currentThumbPath: String?,
    ) : AttachmentDataProvider {
        private var currentTransferData: TransferData? = null
        override val attachment: SceytAttachment get() = currentAttachment
        override val size: Size? get() = null
        override val blurredThumb: Bitmap? get() = null
        override val thumbPath: String? get() = currentThumbPath
        override val duration: Long? get() = null
        override val audioMetadata: AudioMetadata? get() = null
        override val transferData: TransferData? get() = currentTransferData

        override fun updateAttachment(file: SceytAttachment): SceytAttachment {
            currentAttachment = file
            return currentAttachment
        }

        override fun updateTransferData(transferData: TransferData?) {
            currentTransferData = transferData
        }

        override fun updateThumbPath(thumbPath: String?) {
            currentThumbPath = thumbPath
        }
    }

    private companion object {
        const val MESSAGE_TID = 10L
    }
}
