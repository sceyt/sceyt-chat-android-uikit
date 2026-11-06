package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.persistence.extensions.fileTransferData
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.CompletableDeferred
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
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentUploadPreparationTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var previousConfig: SceytChatUIKitConfig
    private val transport = RecordingFileTransferTransport()
    private val attachmentLogic = mock<PersistenceAttachmentLogic>()
    private val saved = mutableMapOf<Long, SceytAttachment>()

    @Before
    fun setUp() {
        previousFileTransfer = SceytChatUIKit.fileTransfer
        previousConfig = SceytChatUIKit.config
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            transport = this@AttachmentUploadPreparationTest.transport
        }
        SceytChatUIKit.config = SceytChatUIKitConfig().apply {
            preventDuplicateAttachmentUpload = false
        }
        doSuspendableAnswer { call ->
            val tid = call.getArgument<Long>(0)
            saved[tid] = attachment(
                messageTid = tid,
                filePath = call.getArgument(1),
                fileSize = call.getArgument(2),
            ).copy(metadata = call.getArgument(3))
            Unit
        }.whenever(attachmentLogic) {
            updateAttachmentFilePathAndMetadata(any(), any(), any(), anyOrNull())
        }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previousFileTransfer
        SceytChatUIKit.config = previousConfig
    }

    @Test
    fun `upload receives prepared app metadata and preserves it on completion`() = runTest {
        val file = temporary.newFile().apply { writeText("plaintext") }
        val value = attachment(filePath = file.path, originalFilePath = file.path)
            .copy(metadata = """{"caption":"keep"}""")
        val task = transferTask(value)
        transport.onPrepareUpload = { request ->
            assertThat(request.attachment.filePath).isEqualTo(file.path)
            assertThat(request.metadata.entrySet()).isEmpty()
            JsonObject().apply { addProperty("appEncryption", "saved-key") }
        }
        val coordinator = AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), attachmentLogic, backgroundScope)

        coordinator.upload(value, task, { _, _ -> }, {})
        runCurrent()

        val call = transport.uploadCalls.single()
        val prepared = call.request.attachment
        val metadata = JsonParser.parseString(prepared.metadata).asJsonObject
        assertThat(metadata.getAsJsonObject("file_transfer").get("appEncryption")?.asString).isEqualTo("saved-key")
        assertThat(call.request.metadata).isEqualTo(metadata.getAsJsonObject("file_transfer"))
        assertThat(saved[value.messageTid]?.metadata).isEqualTo(prepared.metadata)
        assertThat(metadata.get("caption").asString).isEqualTo("keep")
        call.succeed("https://cdn.test/uploaded")
        runCurrent()
        assertThat(JsonParser.parseString(task.attachment.metadata).asJsonObject).isEqualTo(metadata)
    }

    @Test
    fun `upload waits until app data has been saved`() = runTest {
        val file = temporary.newFile().apply { writeText("plaintext") }
        val value = attachment(filePath = file.path)
        val persisted = CompletableDeferred<Unit>()
        doSuspendableAnswer { persisted.await() }.whenever(attachmentLogic) {
            updateAttachmentFilePathAndMetadata(any(), any(), any(), anyOrNull())
        }
        val data = appData()
        transport.onPrepareUpload = { data }
        val coordinator = AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), attachmentLogic, backgroundScope)

        coordinator.upload(value, transferTask(value), { _, _ -> }, {})
        runCurrent()
        assertThat(transport.uploadCalls).isEmpty()

        persisted.complete(Unit)
        runCurrent()
        assertThat(transport.uploadCalls.single().request.metadata).isEqualTo(data)
    }

    @Test
    fun `a new coordinator restores the saved app object without creating another key`() = runTest {
        val file = temporary.newFile().apply { writeText("plaintext") }
        val value = attachment(filePath = file.path)
        val data = appData()
        var created = 0
        SceytChatUIKit.fileTransfer.encryptionEnabled = true
        transport.onPrepareUpload = { request ->
            if (request.metadata.size() == 0) data.also { created++ } else request.metadata
        }
        val first = AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), attachmentLogic, backgroundScope)
        first.upload(value, transferTask(value), { _, _ -> }, {})
        runCurrent()
        transport.uploadCalls.single().interrupt()
        runCurrent()

        val restored = checkNotNull(saved[value.messageTid])
        val task = transferTask(restored)
        val second = AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), attachmentLogic, backgroundScope)
        var result: SceytResponse<String>? = null
        second.upload(restored, task, { _, response -> result = response }, {})
        runCurrent()

        assertThat(created).isEqualTo(1)
        assertThat(transport.uploadCalls.last().request.metadata).isEqualTo(data)
        transport.uploadCalls.last().succeed("https://cdn.test/custom-encrypted")
        runCurrent()
        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        assertThat(task.attachment.metadata.fileTransferData()).isEqualTo(data)
    }

    @Test
    fun `shared callers save the same app object and keep their own metadata`() = runTest {
        val file = temporary.newFile().apply { writeText("shared plaintext") }
        val first = attachment(messageTid = 1, filePath = file.path).copy(metadata = """{"caption":"first"}""")
        val second = first.copy(messageTid = 2, metadata = """{"caption":"second"}""")
        val late = first.copy(messageTid = 3, metadata = """{"caption":"late"}""")
        val data = appData()
        var preparations = 0
        transport.onPrepareUpload = {
            preparations++
            data
        }
        val coordinator = AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), attachmentLogic, backgroundScope)
        val firstTask = transferTask(first).apply { isSharedUpload = true }
        val secondTask = transferTask(second).apply { isSharedUpload = true }
        val lateTask = transferTask(late).apply { isSharedUpload = true }

        coordinator.upload(first, firstTask, { _, _ -> }, {})
        coordinator.upload(second, secondTask, { _, _ -> }, {})
        runCurrent()
        coordinator.upload(late, lateTask, { _, _ -> }, {})
        runCurrent()

        assertThat(preparations).isEqualTo(1)
        assertThat(transport.uploadCalls).hasSize(1)
        for (value in listOf(first, second, late)) {
            val metadata = JsonParser.parseString(checkNotNull(saved[value.messageTid]).metadata).asJsonObject
            assertThat(metadata.getAsJsonObject("file_transfer")).isEqualTo(data)
            assertThat(metadata.get("caption")).isEqualTo(JsonParser.parseString(value.metadata).asJsonObject.get("caption"))
        }
        transport.uploadCalls.single().succeed("shared-url")
        runCurrent()
        assertThat(lateTask.attachment.metadata.fileTransferData()).isEqualTo(data)
    }

    @Test
    fun `preparation failure prevents persistence and upload`() = runTest {
        val file = temporary.newFile().apply { writeText("plaintext") }
        val value = attachment(filePath = file.path)
        transport.onPrepareUpload = { error("Could not prepare") }
        val coordinator = AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), attachmentLogic, backgroundScope)
        var result: SceytResponse<String>? = null

        coordinator.upload(value, transferTask(value), { _, response -> result = response }, {})
        runCurrent()

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(saved).isEmpty()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
    }

    @Test
    fun `persistence failure prevents upload`() = runTest {
        val file = temporary.newFile().apply { writeText("plaintext") }
        val value = attachment(filePath = file.path)
        doSuspendableAnswer { error("Database write failed") }.whenever(attachmentLogic) {
            updateAttachmentFilePathAndMetadata(any(), any(), any(), anyOrNull())
        }
        transport.onPrepareUpload = { appData() }
        val coordinator = AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), attachmentLogic, backgroundScope)
        var result: SceytResponse<String>? = null

        coordinator.upload(value, transferTask(value), { _, response -> result = response }, {})
        runCurrent()

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
    }

    @Test
    fun `file path callbacks run after app data is saved`() = runTest {
        val file = temporary.newFile().apply { writeText("prepared image") }
        val value = attachment(filePath = file.path, type = AttachmentTypeEnum.Image.value)
        val order = mutableListOf<String>()
        val data = appData()
        SceytChatUIKit.fileTransfer.encryptionEnabled = true
        transport.onPrepareUpload = {
            order += "prepare"
            data
        }
        val execution = AttachmentTransferRouter(RuntimeEnvironment.getApplication(), attachmentLogic).upload(
            attachment = value,
            operationId = "upload:42",
            isSharedUpload = false,
            onFilePrepared = { order += "path" },
            preparedFilePath = file.path,
            onUploadPrepared = { prepared ->
                assertThat(prepared.metadata.fileTransferData()).isEqualTo(data)
                order += "saved"
                prepared
            },
        )
        val result = async { execution.execute {} }
        runCurrent()
        transport.uploadCalls.single().succeed("uploaded")

        assertThat(result.await()).isEqualTo(FileTransferResult.Success("uploaded"))
        assertThat(order).containsExactly("prepare", "saved", "path").inOrder()
        assertThat(execution.attachment?.filePath).isEqualTo(file.path)
    }

    @Test
    fun `a completed plain checksum skips transport preparation and upload`() = runTest {
        val file = temporary.newFile().apply { writeText("plaintext") }
        val value = attachment(filePath = file.path, originalFilePath = file.path)
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        whenever(attachmentLogic.getFileChecksumData(file.path))
            .thenReturn(FileChecksumData(1, null, "cached-url", null, file.length()))
        var preparations = 0
        transport.onPrepareUpload = {
            preparations++
            appData()
        }
        var response: SceytResponse<String>? = null
        AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), attachmentLogic, backgroundScope)
            .upload(value, transferTask(value), { _, result -> response = result }, {})
        runCurrent()

        assertThat((response as SceytResponse.Success).data).isEqualTo("cached-url")
        assertThat(preparations).isEqualTo(0)
        assertThat(transport.uploadCalls).isEmpty()
        assertThat(saved).isEmpty()
    }

    private fun appData(): JsonObject = JsonParser.parseString(
        """{"encryption":{"key":"app-key","algorithm":"custom"},"uploadId":"saved-upload","values":[1,true,"text"]}"""
    ).asJsonObject
}
