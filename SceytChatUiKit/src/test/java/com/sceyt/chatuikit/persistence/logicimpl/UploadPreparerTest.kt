package com.sceyt.chatuikit.persistence.logicimpl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.ResizeConfig
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.logger.SceytLoggerImpl
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.shared.media_encoder.TranscodeResultEnum
import com.sceyt.chatuikit.shared.media_encoder.VideoTranscodeData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class UploadPreparerTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private lateinit var previousConfig: SceytChatUIKitConfig
    private val attachmentLogic = mock<PersistenceAttachmentLogic>()
    private val messages = mutableListOf<String?>()

    @Before
    fun setUp() {
        previousConfig = SceytChatUIKit.config
        SceytChatUIKit.config = SceytChatUIKitConfig().apply {
            preventDuplicateAttachmentUpload = false
        }
        SceytLog.setLogger(SceytLogLevel.Verbose) { _, tag, message, _ ->
            if (tag == "UploadPreparer") messages += message
        }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.config = previousConfig
        SceytLog.setLogger(SceytLogLevel.Verbose, SceytLoggerImpl())
    }

    @Test
    fun `generic file keeps its source without logging a resize failure`() = runTest {
        val file = temporary.newFile("document.txt").apply { writeText("document") }
        val attachment = attachment(filePath = file.path, fileSize = file.length())
        val preparer = UploadPreparer(RuntimeEnvironment.getApplication(), mock())

        val result = preparer.prepare(attachment, onProgress = {})

        assertThat(result.attachment).isEqualTo(attachment)
        assertThat(result.preparedFilePath).isNull()
        assertThat(messages).isEmpty()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `failed image resize logs the failure and keeps the source`() = runTest {
        val file = temporary.newFile("invalid.jpg").apply { writeText("invalid image") }
        val attachment = attachment(
            filePath = file.path,
            fileSize = file.length(),
            type = AttachmentTypeEnum.Image.value,
        )
        val preparer = UploadPreparer(RuntimeEnvironment.getApplication(), mock())

        val result = preparer.prepare(attachment, onProgress = {})

        assertThat(result.attachment).isEqualTo(attachment)
        assertThat(result.preparedFilePath).isNull()
        assertThat(messages).containsExactly(
            "Couldn't resize file with reason Invalid image dimensions for resizing ${file.path}"
        )
    }

    @Test
    fun `preview attachments bypass preparation and checksum lookup`() = runTest {
        val preview = attachment(type = AttachmentTypeEnum.Preview.value, filePath = "preview.jpg")
        val preparer = UploadPreparer(RuntimeEnvironment.getApplication(), attachmentLogic)

        val result = preparer.prepare(preview, onProgress = {})

        assertThat(result.attachment).isEqualTo(preview)
        assertThat(result.preparedFilePath).isNull()
        assertThat(result.reusedUrl).isNull()
        verifyNoInteractions(attachmentLogic)
    }

    @Test
    fun `cached upload URL completes preparation without requiring a local source`() = runTest {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        val original = attachment(filePath = "/missing/original.mp4", type = AttachmentTypeEnum.Video.value)
        whenever(attachmentLogic.getFileChecksumData(original.filePath)).thenReturn(
            FileChecksumData(1L, "/cached/video.mp4", "cached-url", null, 123L)
        )

        val result = UploadPreparer(RuntimeEnvironment.getApplication(), attachmentLogic)
            .prepare(original, onProgress = {})

        assertThat(result.reusedUrl).isEqualTo("cached-url")
        assertThat(result.preparedFilePath).isEqualTo("/cached/video.mp4")
        assertThat(result.attachment).isEqualTo(original)
    }

    @Test
    fun `encrypted preparation reuses cached media but ignores its uploaded URL`() = runTest {
        val source = temporary.newFile("source.mp4").apply { writeText("source") }
        val prepared = temporary.newFile("prepared.mp4").apply { writeText("prepared video") }
        val original = attachment(filePath = source.path, type = AttachmentTypeEnum.Video.value)
        whenever(attachmentLogic.getFileChecksumData(source.path)).thenReturn(
            FileChecksumData(1L, prepared.path, "old-encrypted-url", null, prepared.length())
        )

        val result = UploadPreparer(RuntimeEnvironment.getApplication(), attachmentLogic)
            .prepare(original, onProgress = {}, reuseUploadedUrl = false)

        assertThat(result.reusedUrl).isNull()
        assertThat(result.preparedFilePath).isEqualTo(prepared.path)
        assertThat(result.attachment.filePath).isEqualTo(prepared.path)
        assertThat(result.attachment.fileSize).isEqualTo(prepared.length())
        assertThat(result.attachment.originalFilePath).isEqualTo(source.path)
    }

    @Test
    fun `restart reuses the saved prepared file without another checksum lookup`() = runTest {
        val source = temporary.newFile("original.mp4").apply { writeText("source") }
        val prepared = temporary.newFile("saved.mp4").apply { writeText("prepared") }
        val original = attachment(filePath = prepared.path, type = AttachmentTypeEnum.Video.value)
            .copy(originalFilePath = source.path)

        val result = UploadPreparer(RuntimeEnvironment.getApplication(), attachmentLogic)
            .prepare(original, onProgress = {}, reuseUploadedUrl = false)

        assertThat(result.preparedFilePath).isEqualTo(prepared.path)
        assertThat(result.attachment.filePath).isEqualTo(prepared.path)
        verifyNoInteractions(attachmentLogic)
    }

    @Test
    fun `an explicitly prepared file takes precedence over the attachment path`() = runTest {
        val source = temporary.newFile("source.jpg").apply { writeText("source") }
        val prepared = temporary.newFile("prepared.jpg").apply { writeText("prepared") }
        val original = attachment(filePath = source.path, type = AttachmentTypeEnum.Image.value)

        val result = UploadPreparer(RuntimeEnvironment.getApplication(), attachmentLogic).prepare(
            original,
            onProgress = {},
            reuseUploadedUrl = false,
            preparedFilePath = prepared.path,
        )

        assertThat(result.preparedFilePath).isEqualTo(prepared.path)
        assertThat(result.attachment.fileSize).isEqualTo(prepared.length())
        verifyNoInteractions(attachmentLogic)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `missing prepared files are rebuilt from the original image with configured dimensions`() = runTest {
        val source = temporary.newFile("original.png")
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val missing = File(temporary.root, "missing.jpg")
        SceytChatUIKit.config.imageAttachmentResizeConfig = ResizeConfig(32, 90)
        val original = attachment(filePath = missing.path, type = AttachmentTypeEnum.Image.value)
            .copy(originalFilePath = source.path)
        whenever(attachmentLogic.getFileChecksumData(source.path)).thenReturn(
            FileChecksumData(1L, missing.path, "old-url", null, 0L)
        )

        val result = UploadPreparer(RuntimeEnvironment.getApplication(), attachmentLogic).prepare(
            original,
            onProgress = {},
            reuseUploadedUrl = false,
            preparedFilePath = missing.path,
        )

        val prepared = File(checkNotNull(result.preparedFilePath))
        try {
            val resized = checkNotNull(BitmapFactory.decodeFile(prepared.path))
            assertThat(resized.width).isEqualTo(32)
            assertThat(resized.height).isEqualTo(32)
            resized.recycle()
            assertThat(result.attachment.filePath).isEqualTo(prepared.path)
            assertThat(result.attachment.fileSize).isEqualTo(prepared.length())
            assertThat(result.attachment.originalFilePath).isEqualTo(source.path)
            assertThat(result.reusedUrl).isNull()
        } finally {
            prepared.delete()
        }
    }

    @Test
    fun `missing blank nonexistent and directory sources are rejected`() = runTest {
        val directory = temporary.newFolder("directory")
        val missing = File(temporary.root, "missing.txt")
        for (path in listOf(null, "", missing.path, directory.path)) {
            val failure = runCatching {
                UploadPreparer(RuntimeEnvironment.getApplication(), attachmentLogic)
                    .prepare(attachment(filePath = path), onProgress = {})
            }.exceptionOrNull()

            assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `video preparation forwards progress and accepts its terminal callback only once`() = runTest {
        val source = temporary.newFile("source.mp4").apply { writeText("source") }
        val prepared = temporary.newFile("prepared.mp4").apply { writeText("prepared video") }
        var progressCallback: ((VideoTranscodeData) -> Unit)? = null
        var resultCallback: ((Result<String>) -> Unit)? = null
        val progress = mutableListOf<Float>()
        mockTranscode { onProgress, onResult ->
            progressCallback = onProgress
            resultCallback = onResult
            prepared
        }.use {
            val result = async {
                UploadPreparer(RuntimeEnvironment.getApplication(), attachmentLogic).prepare(
                    attachment(filePath = source.path, type = AttachmentTypeEnum.Video.value),
                    onProgress = { progress += it },
                )
            }
            runCurrent()
            checkNotNull(progressCallback).invoke(VideoTranscodeData(TranscodeResultEnum.Progress, progressPercent = 45f))
            checkNotNull(resultCallback).invoke(Result.success(prepared.path))
            resultCallback.invoke(Result.failure(IOException("late callback")))

            val uploaded = result.await()
            assertThat(uploaded.preparedFilePath).isEqualTo(prepared.path)
            assertThat(uploaded.attachment.fileSize).isEqualTo(prepared.length())
            assertThat(progress).containsExactly(45f)
        }
    }

    @Test
    fun `failed video transcode logs the failure and uploads the original source`() = runTest {
        val source = temporary.newFile("source.mp4").apply { writeText("source") }
        val original = attachment(filePath = source.path, type = AttachmentTypeEnum.Video.value)
        mockTranscode { _, onResult ->
            onResult(Result.failure(IOException("transcode failed")))
            null
        }.use {
            val result = UploadPreparer(RuntimeEnvironment.getApplication(), attachmentLogic)
                .prepare(original, onProgress = {})

            assertThat(result.attachment).isEqualTo(original)
            assertThat(result.preparedFilePath).isNull()
            assertThat(messages).containsExactly("Couldn't resize file with reason transcode failed")
        }
    }

    private fun mockTranscode(
        onStart: ((VideoTranscodeData) -> Unit, (Result<String>) -> Unit) -> File?,
    ): MockedStatic<Any> {
        @Suppress("UNCHECKED_CAST")
        val extensions = Class.forName("com.sceyt.chatuikit.persistence.extensions.AttachmentResizeExtensionsKt") as Class<Any>
        return Mockito.mockStatic(extensions) { invocation ->
            if (invocation.method.name == "transcodeVideo")
                onStart(invocation.getArgument(3), invocation.getArgument(4))
            else invocation.callRealMethod()
        }
    }
}
