package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.download.ChatSdkFileDownloader
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFileTransferProviderTest {
    private lateinit var utilityConstruction: MockedConstruction<FileTransferUtility>
    private lateinit var downloaderConstruction: MockedConstruction<ChatSdkFileDownloader>

    @Before
    fun setUp() {
        utilityConstruction = Mockito.mockConstruction(FileTransferUtility::class.java)
        downloaderConstruction = Mockito.mockConstruction(ChatSdkFileDownloader::class.java)
    }

    @After
    fun tearDown() {
        downloaderConstruction.close()
        utilityConstruction.close()
    }

    @Test
    fun `upload uses prepared source and returns result`() = runTest {
        val transport = DefaultFileTransferProvider()
        val utility = utilityConstruction.constructed().single()
        val sourceFile = File("/tmp/prepared.txt")
        val request = FileUploadRequest(
            operationId = "upload:10",
            sourceFile = sourceFile,
            attachment = attachment(filePath = "/tmp/original.txt"),
        )
        val progress = mutableListOf<Float>()
        val sourceCaptor = argumentCaptor<File>()
        doSuspendableAnswer { invocation ->
            invocation.getArgument<(Float) -> Unit>(1)(35f)
            SceytResponse.Success("uploaded-url")
        }.whenever(utility) { uploadFile(any(), any()) }

        val result = transport.upload(request, FileTransferCallback(progress::add))

        verify(utility).uploadFile(sourceCaptor.capture(), any())
        assertThat(sourceCaptor.firstValue).isEqualTo(sourceFile)
        assertThat(progress).containsExactly(35f)
        assertThat(result).isEqualTo(FileTransferResult.Success("uploaded-url"))
    }

    @Test
    fun `upload forwards SDK failure`() = runTest {
        val transport = DefaultFileTransferProvider()
        val utility = utilityConstruction.constructed().single()
        val request = FileUploadRequest(
            "upload:10",
            File("/tmp/prepared.txt"),
            attachment(filePath = "/tmp/prepared.txt")
        )
        val error = SceytException(1, "upload failed")
        whenever(utility.uploadFile(any(), any())).thenReturn(SceytResponse.Error(error))

        val result = transport.upload(request) {}

        assertThat((result as FileTransferResult.Failure).error).isSameInstanceAs(error)
    }

    @Test
    fun `null SDK upload reference returns failure`() = runTest {
        val transport = DefaultFileTransferProvider()
        val utility = utilityConstruction.constructed().single()
        whenever(utility.uploadFile(any(), any())).thenReturn(SceytResponse.Success(null))
        val request = FileUploadRequest(
            "upload:10",
            File("/tmp/prepared.txt"),
            attachment(filePath = "/tmp/prepared.txt")
        )

        val result = transport.upload(request) {}

        assertThat((result as FileTransferResult.Failure).error.message)
            .isEqualTo("File upload failed")
    }

    @Test
    fun `cancelling upload cancels its suspended SDK bridge`() = runTest {
        val transport = DefaultFileTransferProvider()
        val utility = utilityConstruction.constructed().single()
        val request = FileUploadRequest(
            "upload:10",
            File("/tmp/prepared.txt"),
            attachment(filePath = "/tmp/prepared.txt")
        )
        var bridgeCancelled = false
        doSuspendableAnswer {
            try {
                awaitCancellation()
            } finally {
                bridgeCancelled = true
            }
        }.whenever(utility) { uploadFile(any(), any()) }

        val job = launch { transport.upload(request) {} }
        runCurrent()
        job.cancelAndJoin()

        assertThat(bridgeCancelled).isTrue()
        assertThat(job.isCancelled).isTrue()
    }

    @Test
    fun `download requests and metadata go unchanged to the native downloader`() = runTest {
        val transport = DefaultFileTransferProvider()
        val downloader = downloaderConstruction.constructed().single()
        val customMetadata = JsonObject().apply { addProperty("appEncryption", "saved-key") }
        for (metadata in listOf(JsonObject(), customMetadata)) {
            val request = FileDownloadRequest(
                "download:10",
                "https://cdn.test/file",
                File("/tmp/downloaded.txt"),
                attachment().copy(metadata = metadata.toString()),
            )
            val expected = FileTransferResult.Success(request.destinationFile.path)
            whenever(downloader.download(any(), any())).thenReturn(expected)

            val result = transport.download(request) {}

            verify(downloader).download(same(request), any())
            assertThat(result).isSameInstanceAs(expected)
        }
    }

    @Test
    fun `download controls delegate to the same native adapter`() {
        val transport = DefaultFileTransferProvider()
        val downloader = downloaderConstruction.constructed().single()
        whenever(downloader.pause("download:10")).thenReturn(true)
        whenever(downloader.resume("download:10")).thenReturn(true)

        assertThat(transport.pause("download:10")).isTrue()
        assertThat(transport.resume("download:10")).isTrue()
        assertThat(transport.pause("upload:10")).isFalse()
        transport.cancel("download:10")

        verify(downloader).cancel("download:10")
    }
}
