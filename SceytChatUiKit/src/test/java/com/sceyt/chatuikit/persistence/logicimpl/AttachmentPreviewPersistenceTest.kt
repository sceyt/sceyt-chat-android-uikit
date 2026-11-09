package com.sceyt.chatuikit.persistence.logicimpl

import android.graphics.Bitmap
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toAttachment
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
class AttachmentPreviewPersistenceTest {
    private val context = RuntimeEnvironment.getApplication()
    private val provider = RecordingFileTransferProvider()
    private lateinit var database: SceytDatabase
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SceytDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        previousFileTransfer = SceytChatUIKit.fileTransfer
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            provider = this@AttachmentPreviewPersistenceTest.provider
        }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previousFileTransfer
        database.close()
    }

    @Test
    fun `video transfer updates do not overwrite the stored preview`() = runBlocking {
        storeVideoMessageWithPreview()

        database.attachmentsDao().updateAttachmentAndPayLoad(
            TransferData(
                messageTid = TID,
                progressPercent = 100f,
                state = TransferState.Downloaded,
                filePath = "/videos/video.mp4",
                url = VIDEO_URL
            )
        )

        val attachments = checkNotNull(database.messageDao().getMessageByTid(TID)?.attachments)
            .map { it.toAttachment() }
        val video = attachments.single { it.type == AttachmentTypeEnum.Video.value }
        val preview = attachments.single { it.type == AttachmentTypeEnum.Preview.value }
        assertThat(video.filePath).isEqualTo("/videos/video.mp4")
        assertThat(video.transferState).isEqualTo(TransferState.Downloaded)
        assertThat(preview.url).isEqualTo(PREVIEW_URL)
        assertThat(preview.filePath).isNull()
        assertThat(database.attachmentsDao().getAllAttachmentPayLoadsByMsgTid(TID)).hasSize(1)
    }

    @Test
    fun `preview download uses its own operation and saves the local path`() = runBlocking {
        val video = storeVideoMessageWithPreview()
        val transfer = AttachmentPreviewTransfer(context, database.attachmentsDao(), MessagesCache(), mock())
        val loaded = CompletableDeferred<String>()

        transfer.loadPreviewPath(video) { loaded.complete(it) }

        val call = withTimeout(5_000) {
            while (provider.downloadCalls.isEmpty()) delay(10)
            provider.downloadCalls.single()
        }
        assertThat(call.request.operationId).isEqualTo("download:$TID:preview")
        assertThat(call.request.url).isEqualTo(PREVIEW_URL)
        val file = call.request.destinationFile.apply {
            parentFile?.mkdirs()
            writeText("jpeg")
        }
        call.succeed(file.path)

        assertThat(withTimeout(5_000) { loaded.await() }).isEqualTo(file.path)
        assertThat(transfer.getPreview(video)?.filePath).isEqualTo(file.path)
    }

    @Test
    fun `video uses the preview its metadata links to`() = runBlocking {
        val video = storeVideoMessageWithPreview(extraPreviewUrl = "https://cdn.test/other_preview.jpg")
        val transfer = AttachmentPreviewTransfer(context, database.attachmentsDao(), MessagesCache(), mock())

        transfer.loadPreviewPath(video) { }

        val call = withTimeout(5_000) {
            while (provider.downloadCalls.isEmpty()) delay(10)
            provider.downloadCalls.single()
        }
        assertThat(call.request.url).isEqualTo(PREVIEW_URL)
    }

    @Test
    fun `video without a preview link has no preview`() = runBlocking {
        val video = storeVideoMessageWithPreview(linkedPreviewUrl = null)
        val transfer = AttachmentPreviewTransfer(context, database.attachmentsDao(), MessagesCache(), mock())

        assertThat(transfer.getPreview(video)).isNull()
    }

    @Test
    fun `cancelling all transfers stops a running preview download`() = runBlocking {
        val video = storeVideoMessageWithPreview()
        val transfer = AttachmentPreviewTransfer(context, database.attachmentsDao(), MessagesCache(), mock())
        var loaded: String? = null

        transfer.loadPreviewPath(video) { loaded = it }
        val call = withTimeout(5_000) {
            while (provider.downloadCalls.isEmpty()) delay(10)
            provider.downloadCalls.single()
        }
        transfer.cancelAll()

        withTimeout(5_000) { while (!call.cancelled) delay(10) }
        call.succeed("/late/preview.jpg")
        assertThat(loaded).isNull()
        assertThat(transfer.getPreview(video)?.filePath).isNull()
    }

    @Test
    fun `a thrown preview preparation error is returned as a failed upload`() = runBlocking {
        provider.onPrepareUpload = { throw IllegalStateException("encryption unavailable") }
        ShadowMediaMetadataRetriever.addFrame(
            DataSource.toDataSource(VIDEO_SOURCE),
            1_000L,
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888),
        )
        val transfer = AttachmentPreviewTransfer(context, database.attachmentsDao(), MessagesCache(), mock())
        val video = attachment(
            messageTid = TID,
            name = "video.mp4",
            type = AttachmentTypeEnum.Video.value,
            url = null,
            filePath = VIDEO_SOURCE,
        )

        val result = transfer.upload(video)

        assertThat(result).isInstanceOf(PreviewUploadResult.Failed::class.java)
        assertThat((result as PreviewUploadResult.Failed).error).hasMessageThat()
            .isEqualTo("encryption unavailable")
        assertThat(provider.uploadCalls).isEmpty()
    }

    private suspend fun storeVideoMessageWithPreview(
        extraPreviewUrl: String? = null,
        linkedPreviewUrl: String? = PREVIEW_URL,
    ): SceytAttachment {
        val metadata = linkedPreviewUrl?.let { JSONObject().put(SceytConstants.Preview, it).toString() }
        val video = attachment(
            messageTid = TID,
            name = "video.mp4",
            type = AttachmentTypeEnum.Video.value,
            url = VIDEO_URL,
            filePath = null,
            state = TransferState.PendingDownload,
        ).copy(metadata = metadata)
        val previews = listOfNotNull(extraPreviewUrl, PREVIEW_URL).map { url ->
            attachment(
                messageTid = TID,
                name = "video_preview.jpg",
                type = AttachmentTypeEnum.Preview.value,
                url = url,
                filePath = null,
                state = null,
            )
        }
        val message = createMessage(createdAt = 1L, id = TID, tid = TID)
            .copy(attachments = listOf(video) + previews)
        database.messageDao().upsertMessage(message.toMessageDb(unList = false))
        return video
    }

    private companion object {
        const val TID = 42L
        const val VIDEO_SOURCE = "videos/source.mp4"
        const val VIDEO_URL = "https://cdn.test/video.mp4"
        const val PREVIEW_URL = "https://cdn.test/video_preview.jpg"
    }
}
