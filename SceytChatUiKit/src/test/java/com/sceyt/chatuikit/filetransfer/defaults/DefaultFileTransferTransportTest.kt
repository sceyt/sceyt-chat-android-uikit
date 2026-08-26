package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferEvent
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.TransferRole
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFileTransferTransportTest {
    private lateinit var utilityConstruction: MockedConstruction<FileTransferUtility>

    @Before
    fun setUp() {
        utilityConstruction = Mockito.mockConstruction(FileTransferUtility::class.java)
    }

    @After
    fun tearDown() {
        utilityConstruction.close()
    }

    @Test
    fun `native pause and resume are unsupported`() {
        val transport = DefaultFileTransferTransport()

        assertThat(transport.pause("upload:10")).isFalse()
        assertThat(transport.resume("upload:10")).isFalse()
    }

    @Test
    fun `upload uses prepared source and returns result`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val sourceFile = File("/tmp/prepared.txt")
        val request = FileUploadRequest(
            operationId = "upload:10",
            sourceFile = sourceFile,
            fileName = "prepared.txt",
            mimeType = "text/plain",
            attachment = attachment(filePath = "/tmp/original.txt"),
        )
        val events = mutableListOf<FileTransferEvent>()
        val attachmentCaptor = argumentCaptor<SceytAttachment>()
        doSuspendableAnswer { invocation ->
            invocation.getArgument<(Float) -> Unit>(1)(35f)
            SceytResponse.Success("uploaded-url")
        }.whenever(utility) { uploadFile(any(), any()) }

        val result = transport.upload(request, FileTransferCallback(events::add))

        verify(utility).uploadFile(attachmentCaptor.capture(), any())

        assertThat(attachmentCaptor.firstValue.filePath).isEqualTo(sourceFile.path)
        assertThat(events).containsExactly(FileTransferEvent.Progress(35f))
        assertThat(result).isEqualTo("uploaded-url")
    }

    @Test
    fun `upload forwards SDK failure`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val request = FileUploadRequest(
            operationId = "upload:10",
            sourceFile = File("/tmp/prepared.txt"),
            fileName = "prepared.txt",
            mimeType = "text/plain",
            attachment = attachment(),
        )
        val error = SceytException(1, "upload failed")
        whenever(utility.uploadFile(any(), any())).thenReturn(SceytResponse.Error(error))

        val result = runCatching { transport.upload(request) { } }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `download uses request data and throws transport failure`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val destination = File("/tmp/downloaded.txt")
        val request = FileDownloadRequest(
            operationId = "download:10",
            url = "https://new.test/file",
            destinationFile = destination,
            attachment = attachment(url = "https://old.test/file"),
        )
        val attachmentCaptor = argumentCaptor<SceytAttachment>()
        val destinationCaptor = argumentCaptor<File>()
        val resultCaptor = argumentCaptor<(SceytResponse<String>) -> Unit>()
        val error = SceytException(12, "download failed")

        val result = async {
            runCatching {
                transport.download(request) { }
            }
        }
        runCurrent()

        verify(utility).downloadFile(
            attachmentCaptor.capture(),
            destinationCaptor.capture(),
            any(),
            resultCaptor.capture(),
        )
        resultCaptor.firstValue(SceytResponse.Error(error))

        assertThat(attachmentCaptor.firstValue.url).isEqualTo(request.url)
        assertThat(destinationCaptor.firstValue).isEqualTo(destination)
        assertThat(result.await().exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `cancelling upload cancels its suspended SDK bridge`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val request = FileUploadRequest(
            operationId = "upload:10",
            sourceFile = File("/tmp/prepared.txt"),
            fileName = "prepared.txt",
            mimeType = "text/plain",
            attachment = attachment(),
        )
        var bridgeCancelled = false
        doSuspendableAnswer {
            try {
                awaitCancellation()
            } finally {
                bridgeCancelled = true
            }
        }.whenever(utility) { uploadFile(any(), any()) }

        val job = launch {
            transport.upload(request) { }
        }
        runCurrent()
        verify(utility).uploadFile(any(), any())

        job.cancelAndJoin()
        assertThat(bridgeCancelled).isTrue()
        assertThat(job.isCancelled).isTrue()
    }

    @Test
    fun `thumbnail upload uses prepared source directly and leaves cleanup to its owner`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val sourceFile = File.createTempFile("poster", ".jpeg").apply { writeText("poster") }
        val request = FileUploadRequest(
            operationId = "upload:10:thumb",
            sourceFile = sourceFile,
            fileName = sourceFile.name,
            mimeType = "image/jpeg",
            attachment = attachment(),
            role = TransferRole.Thumbnail,
        )
        val attachmentCaptor = argumentCaptor<SceytAttachment>()
        whenever(utility.uploadFile(any(), any())).thenReturn(SceytResponse.Success("thumb-url"))

        try {
            assertThat(transport.upload(request) {}).isEqualTo("thumb-url")

            verify(utility).uploadFile(attachmentCaptor.capture(), any())
            assertThat(attachmentCaptor.firstValue.filePath).isEqualTo(sourceFile.path)
            assertThat(sourceFile.readText()).isEqualTo("poster")
        } finally {
            sourceFile.delete()
        }
    }

    @Test
    fun `cancelled thumbnail upload leaves source cleanup to its owner`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val sourceFile = File.createTempFile("poster", ".jpeg").apply { writeText("poster") }
        val request = FileUploadRequest(
            operationId = "upload:11:thumb",
            sourceFile = sourceFile,
            fileName = sourceFile.name,
            mimeType = "image/jpeg",
            attachment = attachment(),
            role = TransferRole.Thumbnail,
        )
        val attachmentCaptor = argumentCaptor<SceytAttachment>()
        doSuspendableAnswer { awaitCancellation() }
            .whenever(utility) { uploadFile(any(), any()) }

        val job = launch { transport.upload(request) { } }
        runCurrent()
        try {
            verify(utility).uploadFile(attachmentCaptor.capture(), any())
            assertThat(attachmentCaptor.firstValue.filePath).isEqualTo(sourceFile.path)

            job.cancelAndJoin()

            assertThat(sourceFile.exists()).isTrue()
        } finally {
            job.cancelAndJoin()
            sourceFile.delete()
        }
    }

    @Test
    fun `cancelling download pauses default utility and ignores late result`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val request = FileDownloadRequest(
            operationId = "download:10",
            url = "https://cdn.test/file",
            destinationFile = File("/tmp/downloaded.txt"),
            attachment = attachment(),
        )
        val resultCaptor = argumentCaptor<(SceytResponse<String>) -> Unit>()

        val job = launch {
            transport.download(request) { }
        }
        runCurrent()
        verify(utility).downloadFile(any(), any(), any(), resultCaptor.capture())

        job.cancelAndJoin()
        resultCaptor.firstValue(SceytResponse.Success("late-result"))

        verify(utility).pauseDownload(any())
        assertThat(job.isCancelled).isTrue()
    }
}
