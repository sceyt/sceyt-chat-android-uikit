package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferEvent
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
import org.mockito.kotlin.verify
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
        val progressCaptor = argumentCaptor<(Float) -> Unit>()
        val resultCaptor = argumentCaptor<(SceytResponse<String>) -> Unit>()

        val result = async {
            transport.upload(request, FileTransferCallback(events::add))
        }
        runCurrent()

        verify(utility).uploadFile(
            attachmentCaptor.capture(),
            progressCaptor.capture(),
            resultCaptor.capture(),
        )
        progressCaptor.firstValue(35f)
        resultCaptor.firstValue(SceytResponse.Success("uploaded-url"))

        assertThat(attachmentCaptor.firstValue.filePath).isEqualTo(sourceFile.path)
        assertThat(events).containsExactly(FileTransferEvent.Progress(35f))
        assertThat(result.await()).isEqualTo("uploaded-url")
    }

    @Test
    fun `duplicate upload results complete once`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val request = FileUploadRequest(
            operationId = "upload:10",
            sourceFile = File("/tmp/prepared.txt"),
            fileName = "prepared.txt",
            mimeType = "text/plain",
            attachment = attachment(),
        )
        val resultCaptor = argumentCaptor<(SceytResponse<String>) -> Unit>()

        val result = async {
            transport.upload(request) { }
        }
        runCurrent()
        verify(utility).uploadFile(any(), any(), resultCaptor.capture())

        resultCaptor.firstValue(SceytResponse.Success("uploaded-url"))
        resultCaptor.firstValue(SceytResponse.Error(SceytException(1, "late failure")))

        assertThat(result.await()).isEqualTo("uploaded-url")
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
    fun `cancelling upload pauses default utility and ignores late result`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val request = FileUploadRequest(
            operationId = "upload:10",
            sourceFile = File("/tmp/prepared.txt"),
            fileName = "prepared.txt",
            mimeType = "text/plain",
            attachment = attachment(),
        )
        val resultCaptor = argumentCaptor<(SceytResponse<String>) -> Unit>()

        val job = launch {
            transport.upload(request) { }
        }
        runCurrent()
        verify(utility).uploadFile(any(), any(), resultCaptor.capture())

        job.cancelAndJoin()
        resultCaptor.firstValue(SceytResponse.Success("late-result"))

        verify(utility).pauseUpload(any())
        assertThat(job.isCancelled).isTrue()
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
