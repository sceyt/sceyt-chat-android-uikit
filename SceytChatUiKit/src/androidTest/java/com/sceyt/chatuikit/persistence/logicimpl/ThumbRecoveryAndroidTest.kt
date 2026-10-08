package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferListeners
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ThumbCallback
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class ThumbRecoveryAndroidTest {
    private lateinit var context: Context
    private lateinit var dir: File
    private lateinit var fileTransferService: FakeFileTransferService
    private lateinit var logic: FileTransferLogicImpl
    private val thumbPaths = CopyOnWriteArrayList<String>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dir = File(context.filesDir, "thumb_recovery_test").apply { mkdirs() }
        fileTransferService = FakeFileTransferService()
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<FileTransferService> { fileTransferService }
            })
        }
        logic = FileTransferLogicImpl(context, unused(), AttachmentThumbPathResolver)
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
        thumbPaths.forEach { File(it).delete() }
        dir.deleteRecursively()
    }

    @Test
    fun cacheWipedThumbIsRegeneratedFromOriginal() {
        val original = writeJpeg(File(dir, "original.jpg"))
        val attachment = attachment(filePath = original.path, state = TransferState.Downloaded)
        fileTransferService.task = taskFor(attachment)

        logic.getAttachmentThumb(attachment.messageTid, attachment, thumbData())
        File(thumbPaths.single()).delete()
        logic.getAttachmentThumb(attachment.messageTid, attachment, thumbData())

        assertThat(thumbPaths).hasSize(2)
        assertThat(thumbPaths.first()).startsWith(context.cacheDir.path)
        assertThat(thumbPaths.last()).isNotEqualTo(thumbPaths.first())
        assertThat(BitmapFactory.decodeFile(thumbPaths.last())).isNotNull()
        assertThat(fileTransferService.downloads).isEmpty()
    }

    @Test
    fun deletedImageOriginalIsDownloadedAgain() {
        val attachment = attachment(
            filePath = File(dir, "deleted.jpg").path,
            state = TransferState.Uploaded,
        )
        fileTransferService.task = taskFor(attachment)

        logic.getAttachmentThumb(attachment.messageTid, attachment, thumbData())

        assertThat(thumbPaths).isEmpty()
        assertThat(fileTransferService.downloads).containsExactly(attachment)
    }

    @Test
    fun deletedVideoOriginalIsDownloadedAgain() {
        val attachment = attachment(
            filePath = File(dir, "deleted.mp4").path,
            state = TransferState.Downloaded,
            type = AttachmentTypeEnum.Video,
            name = "deleted.mp4",
        )
        fileTransferService.task = taskFor(attachment)

        logic.getAttachmentThumb(attachment.messageTid, attachment, thumbData())

        assertThat(thumbPaths).isEmpty()
        assertThat(fileTransferService.downloads).containsExactly(attachment)
    }

    private fun taskFor(attachment: SceytAttachment): TransferTask {
        val task = TransferTask(attachment, attachment.messageTid, attachment.transferState)
        task.thumbCallback = ThumbCallback { path, _ -> thumbPaths.add(path) }
        return task
    }

    private fun writeJpeg(file: File): File {
        val bitmap = Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file
    }

    private fun thumbData() = ThumbData(
        key = ThumbFor.MessagesLisView.value,
        filePath = null,
        size = Size(300, 300),
    )

    private fun attachment(
        filePath: String,
        state: TransferState,
        type: AttachmentTypeEnum = AttachmentTypeEnum.Image,
        name: String = "image.jpg",
    ) = SceytAttachment(
        id = MESSAGE_TID,
        messageId = MESSAGE_TID,
        messageTid = MESSAGE_TID,
        userId = null,
        name = name,
        type = type.value,
        metadata = null,
        fileSize = 100L,
        createdAt = 1_000L,
        url = "https://cdn.test/$name",
        filePath = filePath,
        transferState = state,
        progressPercent = 100f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private inline fun <reified T> unused(): T {
        return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            error("Unexpected call: ${method.name}")
        } as T
    }

    private class FakeFileTransferService : FileTransferService {
        lateinit var task: TransferTask
        val downloads = CopyOnWriteArrayList<SceytAttachment>()

        override fun findOrCreateTransferTask(attachment: SceytAttachment) = task
        override fun findTransferTask(attachment: SceytAttachment) = task
        override fun download(attachment: SceytAttachment, transferTask: TransferTask) {
            downloads.add(attachment)
        }

        override fun setCustomListener(fileTransferListeners: FileTransferListeners.Listeners) = Unit
        override fun addTransferTask(task: TransferTask) = Unit
        override fun removeTransferTask(messageTid: Long) = Unit
        override fun getTasks(): Map<String, TransferTask> = emptyMap()
        override fun clearPreparingThumbPaths() = Unit
        override fun upload(attachment: SceytAttachment, transferTask: TransferTask) = Unit
        override fun uploadSharedFile(attachment: SceytAttachment, transferTask: TransferTask) = Unit
        override fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState) = Unit
        override fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState) = Unit
        override fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData) = Unit
    }

    private companion object {
        const val MESSAGE_TID = 10L
    }
}
