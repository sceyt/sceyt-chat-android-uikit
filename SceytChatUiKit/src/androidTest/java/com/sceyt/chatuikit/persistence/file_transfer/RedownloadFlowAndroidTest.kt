package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.View
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logicimpl.AttachmentThumbPathResolver
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferLogicImpl
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata
import com.sceyt.chatuikit.presentation.helpers.AttachmentViewHolderHelper
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext

@RunWith(AndroidJUnit4::class)
class RedownloadFlowAndroidTest {
    private lateinit var dir: File
    private lateinit var helper: AttachmentViewHolderHelper
    private val updates = CopyOnWriteArrayList<TransferData>()
    private val observer = Observer<TransferData> { updates.add(it) }

    @Before
    fun setUp() {
        dir = File(context.filesDir, "redownload_flow_test").apply { mkdirs() }
        AttachmentTransferStateStore.clear()
        attachmentLogicCalls.clear()
        capturedDownloads.clear()
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<FileTransferService> { service }
                single<PersistenceAttachmentLogic> { attachmentLogic }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) {
                    Dispatchers.IO.limitedParallelism(1)
                }
            })
        }
        service.setCustomListener(CapturingListeners())
        onMain {
            helper = AttachmentViewHolderHelper(View(context))
            FileTransferHelper.onTransferUpdatedLiveData.observeForever(observer)
        }
        updates.clear()
    }

    @After
    fun tearDown() {
        onMain { FileTransferHelper.onTransferUpdatedLiveData.removeObserver(observer) }
        AttachmentTransferStateStore.clear()
        SceytKoinApp.koinApp = null
        stopKoin()
        dir.deleteRecursively()
    }

    @Test
    fun deletedOriginalIsRedownloadedWithProgressAndNewThumb() {
        val item = TestAttachmentItem(uploadedAttachment(File(dir, "deleted-success.jpg").path))
        AttachmentTransferStateStore.put(item.attachment.toUploadedTransferData())

        logic.getAttachmentThumb(MESSAGE_TID, item.attachment, thumbData())

        val (pendingAttachment, task) = awaitDownloadStarted()
        assertThat(pendingAttachment.transferState).isEqualTo(TransferState.PendingDownload)
        assertThat(task.state).isEqualTo(TransferState.PendingDownload)
        awaitUpdate(TransferState.PendingDownload)
        assertThat(applyUpdates(item).state).isEqualTo(TransferState.PendingDownload)

        task.progressCallback?.onProgress(TransferData(MESSAGE_TID, 30f, TransferState.Downloading, null, URL))
        awaitUpdate(TransferState.Downloading)
        val progress = applyUpdates(item)
        assertThat(progress.state).isEqualTo(TransferState.Downloading)
        assertThat(progress.progressPercent).isEqualTo(30f)

        val downloaded = writeJpeg(File(dir, "downloaded.jpg"))
        task.downloadCallback?.onResult(SceytResponse.Success(downloaded.path))
        awaitUpdate(TransferState.Downloaded)
        assertThat(applyUpdates(item).state).isEqualTo(TransferState.Downloaded)
        assertThat(item.attachment.filePath).isEqualTo(downloaded.path)

        logic.getAttachmentThumb(MESSAGE_TID, item.attachment, thumbData())
        val thumb = awaitUpdate(TransferState.ThumbLoaded)
        assertThat(File(thumb.filePath!!).exists()).isTrue()
        awaitCondition { attachmentLogicCalls.any { it.first == "updateAttachmentWithTransferData" } }
        assertThat(attachmentLogicCalls.first { it.first == "resetToPendingDownload" }.second.state)
            .isEqualTo(TransferState.PendingDownload)
    }

    @Test
    fun failedRedownloadReachesUiAsErrorDownload() {
        val item = TestAttachmentItem(uploadedAttachment(File(dir, "deleted-error.jpg").path))
        AttachmentTransferStateStore.put(item.attachment.toUploadedTransferData())

        logic.getAttachmentThumb(MESSAGE_TID, item.attachment, thumbData())
        val (_, task) = awaitDownloadStarted()
        awaitUpdate(TransferState.PendingDownload)
        applyUpdates(item)

        task.downloadCallback?.onResult(SceytResponse.Error(SceytException(0, "offline")))
        awaitUpdate(TransferState.ErrorDownload)

        assertThat(applyUpdates(item).state).isEqualTo(TransferState.ErrorDownload)
    }

    private fun applyUpdates(item: TestAttachmentItem): TransferData {
        onMain {
            updates.toList().forEach { helper.updateTransferData(it, item) { true } }
            updates.clear()
        }
        return item.transferData!!
    }

    private fun awaitDownloadStarted(): Pair<SceytAttachment, TransferTask> {
        awaitCondition { capturedDownloads.isNotEmpty() }
        return capturedDownloads.single()
    }

    private fun awaitUpdate(state: TransferState): TransferData {
        awaitCondition { updates.any { it.state == state } }
        return updates.last { it.state == state }
    }

    private fun awaitCondition(condition: () -> Boolean) {
        repeat(50) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if (condition()) return
            Thread.sleep(100)
        }
        error("Condition was not met in time")
    }

    private fun onMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    private fun writeJpeg(file: File): File {
        val bitmap = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file
    }

    private fun thumbData() = ThumbData(
        key = ThumbFor.MessagesLisView.value,
        filePath = null,
        size = Size(300, 300),
    )

    private fun uploadedAttachment(path: String) = SceytAttachment(
        id = MESSAGE_TID,
        messageId = MESSAGE_TID,
        messageTid = MESSAGE_TID,
        userId = null,
        name = "image.jpg",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 100L,
        createdAt = 1_000L,
        url = URL,
        filePath = path,
        transferState = TransferState.Uploaded,
        progressPercent = 100f,
        originalFilePath = path,
        linkPreviewDetails = null,
    )

    private fun SceytAttachment.toUploadedTransferData() =
        TransferData(messageTid, 100f, TransferState.Uploaded, filePath, url)

    private class CapturingListeners : FileTransferListeners.Listeners {
        override fun download(attachment: SceytAttachment, transferTask: TransferTask) {
            capturedDownloads.add(attachment to transferTask)
        }

        override fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData) {
            logic.getAttachmentThumb(messageTid, attachment, thumbData)
        }

        override fun upload(attachment: SceytAttachment, transferTask: TransferTask) = Unit
        override fun uploadSharedFile(attachment: SceytAttachment, transferTask: TransferTask) = Unit
        override fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState) = Unit
        override fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState) = Unit
    }

    private class TestAttachmentItem(private var currentAttachment: SceytAttachment) : AttachmentDataProvider {
        @Volatile
        private var currentTransferData: TransferData? = null

        @Volatile
        private var currentThumbPath: String? = null
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
        const val MESSAGE_TID = 4242L
        const val URL = "https://cdn.test/image.jpg"
        val context: Context get() = ApplicationProvider.getApplicationContext()
        val capturedDownloads = CopyOnWriteArrayList<Pair<SceytAttachment, TransferTask>>()
        val attachmentLogicCalls = CopyOnWriteArrayList<Pair<String, TransferData>>()
        val attachmentLogic: PersistenceAttachmentLogic by lazy {
            Proxy.newProxyInstance(
                PersistenceAttachmentLogic::class.java.classLoader,
                arrayOf(PersistenceAttachmentLogic::class.java)
            ) { _, method, args ->
                val data = args?.firstOrNull() as? TransferData
                if (data != null) attachmentLogicCalls.add(method.name to data)
                Unit
            } as PersistenceAttachmentLogic
        }
        val logic: FileTransferLogicImpl by lazy {
            FileTransferLogicImpl(context, attachmentLogic, AttachmentThumbPathResolver)
        }
        val service: FileTransferServiceImpl by lazy { FileTransferServiceImpl(context, logic) }
    }
}
