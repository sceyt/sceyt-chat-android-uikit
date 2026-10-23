package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferEvent
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
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
import okhttp3.Call
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
            context = AttachmentTransferContext(attachment(filePath = "/tmp/original.txt")),
        )
        val events = mutableListOf<FileTransferEvent>()
        val sourceCaptor = argumentCaptor<File>()
        doSuspendableAnswer { invocation ->
            invocation.getArgument<(Float) -> Unit>(1)(35f)
            SceytResponse.Success("uploaded-url")
        }.whenever(utility) { uploadFile(any(), any()) }

        val result = transport.upload(request, FileTransferCallback(events::add))

        verify(utility).uploadFile(sourceCaptor.capture(), any())

        assertThat(sourceCaptor.firstValue).isEqualTo(sourceFile)
        assertThat(events).containsExactly(FileTransferEvent.Progress(35f))
        assertThat(result.getOrThrow()).isEqualTo("uploaded-url")
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
        )
        val error = SceytException(1, "upload failed")
        whenever(utility.uploadFile(any(), any()))
            .thenReturn(SceytResponse.Error(error))

        val result = transport.upload(request) { }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `download uses request data and returns transport failure`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val destination = File("/tmp/downloaded.txt")
        val request = FileDownloadRequest(
            operationId = "download:10",
            url = "https://new.test/file",
            destinationFile = destination,
            context = AttachmentTransferContext(attachment(url = "https://old.test/file")),
        )
        val requestCaptor = argumentCaptor<FileDownloadRequest>()
        val resultCaptor = argumentCaptor<(SceytResponse<String>) -> Unit>()
        val error = SceytException(12, "download failed")

        val result = async {
            transport.download(request) { }
        }
        runCurrent()

        verify(utility).downloadFile(
            requestCaptor.capture(),
            any(),
            resultCaptor.capture(),
        )
        resultCaptor.firstValue(SceytResponse.Error(error))

        assertThat(requestCaptor.firstValue).isSameInstanceAs(request)
        assertThat(result.await().exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `null SDK upload reference returns failure`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        whenever(utility.uploadFile(any(), any())).thenReturn(SceytResponse.Success(null))
        val request = FileUploadRequest("upload:10", File("/tmp/prepared.txt"), "prepared.txt", "text/plain")

        val result = transport.upload(request) { }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("File upload failed")
    }

    @Test
    fun `download success returns destination and ignores duplicate result`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val request = FileDownloadRequest("download:10", "https://cdn.test/file", File("/tmp/downloaded.txt"))
        val resultCaptor = argumentCaptor<(SceytResponse<String>) -> Unit>()
        val download = async { transport.download(request) { } }
        runCurrent()
        verify(utility).downloadFile(any(), any(), resultCaptor.capture())

        resultCaptor.firstValue(SceytResponse.Success(request.destinationFile.path))
        resultCaptor.firstValue(SceytResponse.Error(SceytException(1, "duplicate failure")))

        assertThat(download.await().getOrThrow()).isEqualTo(request.destinationFile.path)
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
    fun `cancelling download cancels its own call and ignores late result`() = runTest {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val request = FileDownloadRequest(
            operationId = "download:10",
            url = "https://cdn.test/file",
            destinationFile = File("/tmp/downloaded.txt"),
        )
        val call = Mockito.mock(Call::class.java)
        whenever(utility.downloadFile(any(), any(), any())).thenReturn(call)
        val resultCaptor = argumentCaptor<(SceytResponse<String>) -> Unit>()

        val job = launch {
            transport.download(request) { }
        }
        runCurrent()
        verify(utility).downloadFile(any(), any(), resultCaptor.capture())

        job.cancelAndJoin()
        resultCaptor.firstValue(SceytResponse.Success("late-result"))

        verify(utility).cancelDownload(request.operationId, call)
        assertThat(job.isCancelled).isTrue()
    }
}
