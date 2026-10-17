package com.sceyt.chatuikit.persistence.file_transfer

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
class FileTransferServiceImplRedownloadTest {
    private val downloads = CopyOnWriteArrayList<Pair<SceytAttachment, TransferTask>>()
    private lateinit var service: FileTransferServiceImpl

    @Before
    fun setUp() {
        AttachmentTransferStateStore.clear()
        stopKoin()
        service = FileTransferServiceImpl(RuntimeEnvironment.getApplication(), mock())
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<FileTransferService> { service }
                single<PersistenceAttachmentLogic> { mock() }
                single<FileChecksumDao> { mock() }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) { Dispatchers.Unconfined }
            })
        }
        service.setCustomListener(RecordingListeners(downloads))
    }

    @After
    fun tearDown() {
        AttachmentTransferStateStore.clear()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `redownload resets missing original to pending download and downloads with fresh task`() {
        val attachment = attachment()
        val oldTask = TransferTask(attachment, attachment.messageTid, attachment.transferState)
        service.addTransferTask(oldTask)
        AttachmentTransferStateStore.put(
            TransferData(MESSAGE_TID, 100f, TransferState.Uploaded, attachment.filePath, URL)
        )

        service.redownload(attachment)

        val (downloaded, task) = downloads.single()
        assertThat(downloaded.transferState).isEqualTo(TransferState.PendingDownload)
        assertThat(downloaded.progressPercent).isEqualTo(0f)
        assertThat(downloaded.filePath).isNull()
        assertThat(task).isNotSameInstanceAs(oldTask)
        assertThat(task.state).isEqualTo(TransferState.PendingDownload)
        assertThat(service.findTransferTask(downloaded)).isSameInstanceAs(task)
    }

    @Test
    fun `redownload lets download progress through the state store`() {
        val attachment = attachment()
        AttachmentTransferStateStore.put(
            TransferData(MESSAGE_TID, 100f, TransferState.Uploaded, attachment.filePath, URL)
        )

        service.redownload(attachment)
        val progress = AttachmentTransferStateStore.put(
            TransferData(MESSAGE_TID, 30f, TransferState.Downloading, null, URL)
        )

        assertThat(progress?.state).isEqualTo(TransferState.Downloading)
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
        url = URL,
        filePath = "/gallery/deleted.jpg",
        transferState = TransferState.Uploaded,
        progressPercent = 100f,
        originalFilePath = "/gallery/deleted.jpg",
        linkPreviewDetails = null,
    )

    private class RecordingListeners(
        private val downloads: MutableList<Pair<SceytAttachment, TransferTask>>,
    ) : FileTransferListeners.Listeners {
        override fun download(attachment: SceytAttachment, transferTask: TransferTask) {
            downloads.add(attachment to transferTask)
        }

        override fun upload(attachment: SceytAttachment, transferTask: TransferTask) = Unit
        override fun uploadSharedFile(attachment: SceytAttachment, transferTask: TransferTask) = Unit
        override fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState) = Unit
        override fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState) = Unit
        override fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData) = Unit
    }

    private companion object {
        const val MESSAGE_TID = 9_001L
        const val URL = "https://cdn.test/image.jpg"
    }
}
