package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

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
    fun `upload uses prepared source and translates callbacks`() {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val callback = mock<FileTransferCallback<String>>()
        val sourceFile = File("/tmp/prepared.txt")
        val originalAttachment = attachment(filePath = "/tmp/original.txt")
        val request = FileUploadRequest(
            operationId = "upload:10",
            sourceFile = sourceFile,
            fileName = "prepared.txt",
            mimeType = "text/plain",
            attachment = originalAttachment,
        )
        val attachmentCaptor = argumentCaptor<SceytAttachment>()
        val progressCaptor = argumentCaptor<(Float) -> Unit>()
        val resultCaptor = argumentCaptor<(SceytResponse<String>) -> Unit>()

        transport.upload(request, callback)
        verify(utility).uploadFile(
            attachmentCaptor.capture(),
            progressCaptor.capture(),
            resultCaptor.capture(),
        )
        progressCaptor.firstValue(35f)
        resultCaptor.firstValue(SceytResponse.Success("uploaded-url"))

        assertThat(attachmentCaptor.firstValue.filePath).isEqualTo(sourceFile.path)
        verify(callback).onProgress(35f)
        verify(callback).onSuccess("uploaded-url")
    }

    @Test
    fun `download uses request url and destination and translates failure`() {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val callback = mock<FileTransferCallback<String>>()
        val destination = File("/tmp/downloaded.txt")
        val originalAttachment = attachment(url = "https://old.test/file")
        val request = FileDownloadRequest(
            operationId = "download:10",
            url = "https://new.test/file",
            destinationFile = destination,
            attachment = originalAttachment,
        )
        val attachmentCaptor = argumentCaptor<SceytAttachment>()
        val destinationCaptor = argumentCaptor<File>()
        val progressCaptor = argumentCaptor<(Float) -> Unit>()
        val resultCaptor = argumentCaptor<(SceytResponse<String>) -> Unit>()
        val error = SceytException(12, "download failed")

        transport.download(request, callback)
        verify(utility).downloadFile(
            attachmentCaptor.capture(),
            destinationCaptor.capture(),
            progressCaptor.capture(),
            resultCaptor.capture(),
        )
        resultCaptor.firstValue(SceytResponse.Error(error))

        assertThat(attachmentCaptor.firstValue.url).isEqualTo(request.url)
        assertThat(destinationCaptor.firstValue).isEqualTo(destination)
        verify(callback).onFailure(error)
    }

    @Test
    fun `pause and resume delegate using operation attachment`() {
        val transport = DefaultFileTransferTransport()
        val utility = utilityConstruction.constructed().single()
        val callback = mock<FileTransferCallback<String>>()
        val request = FileUploadRequest(
            operationId = "upload:10",
            sourceFile = File("/tmp/prepared.txt"),
            fileName = "prepared.txt",
            mimeType = "text/plain",
            attachment = attachment(),
        )
        whenever(utility.resumeUpload(any())).thenReturn(true)

        transport.upload(request, callback)

        assertThat(transport.pause(request.operationId)).isTrue()
        assertThat(transport.resume(request.operationId)).isTrue()
        verify(utility).pauseUpload(any())
        verify(utility).resumeUpload(any())
    }

    @Test
    fun `unknown operation cannot pause or resume`() {
        val transport = DefaultFileTransferTransport()

        assertThat(transport.pause("missing")).isFalse()
        assertThat(transport.resume("missing")).isFalse()
    }
}
