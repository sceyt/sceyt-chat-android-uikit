package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class OkHttpDownloaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val client = mock<OkHttpClient>()
    private val firstCall = mock<Call>()
    private val secondCall = mock<Call>()
    private lateinit var builders: MockedConstruction<OkHttpClient.Builder>

    @Before
    fun setUp() {
        builders = Mockito.mockConstruction(
            OkHttpClient.Builder::class.java,
            Mockito.withSettings().defaultAnswer(Mockito.RETURNS_SELF),
        ) { builder, _ ->
            whenever(builder.build()).thenReturn(client)
        }
        whenever(client.newCall(any())).thenReturn(firstCall, secondCall)
    }

    @After
    fun tearDown() {
        builders.close()
    }

    @Test
    fun `same URL with different operation IDs keeps both downloads`() {
        val downloader = OkHttpDownloader()
        val first = request("download:20:first")
        val second = request("download:20:second")

        downloader.downloadFile(first, {}, {})
        downloader.downloadFile(second, {}, {})

        assertThat(downloader.getActiveDownloads())
            .containsExactly(first.operationId, second.operationId)
        downloader.cancelAllDownloads()
    }

    @Test
    fun `pausing one download does not cancel another with the same URL`() {
        val downloader = OkHttpDownloader()
        val first = request("download:20:first")
        val second = request("download:20:second")
        downloader.downloadFile(first, {}, {})
        downloader.downloadFile(second, {}, {})

        downloader.pauseDownload(first.operationId)

        verify(firstCall).cancel()
        verify(secondCall, never()).cancel()
        assertThat(downloader.getActiveDownloads()).containsExactly(second.operationId)
        downloader.cancelAllDownloads()
    }

    @Test
    fun `old failure cannot remove a replacement download`() {
        val downloader = OkHttpDownloader()
        val request = request("download:20:first")
        val callback = argumentCaptor<Callback>()
        downloader.downloadFile(request, {}, {})
        verify(firstCall).enqueue(callback.capture())
        downloader.downloadFile(request, {}, {})

        callback.firstValue.onFailure(firstCall, IOException("old failure"))

        assertThat(downloader.isDownloading(request.operationId)).isTrue()
        downloader.pauseDownload(request.operationId)
        verify(secondCall).cancel()
    }

    @Test
    fun `cancelling an old call does not cancel its replacement`() {
        val downloader = OkHttpDownloader()
        val request = request("download:20:first")
        val oldCall = checkNotNull(downloader.downloadFile(request, {}, {}))
        downloader.downloadFile(request, {}, {})

        downloader.cancelDownload(request.operationId, oldCall)

        verify(firstCall).cancel()
        verify(secondCall, never()).cancel()
        assertThat(downloader.isDownloading(request.operationId)).isTrue()
        downloader.cancelDownload(request.operationId, secondCall)
        verify(secondCall).cancel()
        assertThat(downloader.isDownloading(request.operationId)).isFalse()
    }

    private fun request(operationId: String) = FileDownloadRequest(
        operationId = operationId,
        url = "https://cdn.test/file",
        destinationFile = temporaryFolder.newFile(),
    )
}
