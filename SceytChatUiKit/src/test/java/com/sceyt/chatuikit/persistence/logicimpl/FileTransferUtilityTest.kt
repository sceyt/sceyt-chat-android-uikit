package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class FileTransferUtilityTest {

    @Test
    fun `old upload cleanup does not remove replacement with same message id`() {
        val client = mock<ChatClient>()
        val callbacks = CopyOnWriteArrayList<UrlCallback>()
        val firstUploadStarted = CountDownLatch(1)
        val secondUploadStarted = CountDownLatch(1)
        val uploadCount = AtomicInteger()
        doAnswer { invocation ->
            callbacks += invocation.getArgument<UrlCallback>(2)
            if (uploadCount.incrementAndGet() == 1) {
                firstUploadStarted.countDown()
            } else {
                secondUploadStarted.countDown()
            }
            null
        }.whenever(client).upload(any(), any(), any())

        withChatClient(client) {
            val utility = FileTransferUtility()
            val attachment = attachment(messageTid = 20L)
            val results = CopyOnWriteArrayList<SceytResponse<String>>()

            utility.uploadFile(attachment, {}, results::add)
            assertThat(firstUploadStarted.await(2, TimeUnit.SECONDS)).isTrue()

            utility.uploadFile(attachment, {}, results::add)
            assertThat(secondUploadStarted.await(2, TimeUnit.SECONDS)).isTrue()

            utility.pauseUpload(attachment)
            callbacks.last().onResult("late-result")

            assertThat(results).isEmpty()
        }
    }

    @Test
    fun `sdk error callbacks complete upload once`() {
        val client = mock<ChatClient>()
        val progressCallbacks = CopyOnWriteArrayList<ProgressCallback>()
        val resultCallbacks = CopyOnWriteArrayList<UrlCallback>()
        val uploadStarted = CountDownLatch(1)
        doAnswer { invocation ->
            progressCallbacks += invocation.getArgument<ProgressCallback>(1)
            resultCallbacks += invocation.getArgument<UrlCallback>(2)
            uploadStarted.countDown()
            null
        }.whenever(client).upload(any(), any(), any())

        withChatClient(client) {
            val utility = FileTransferUtility()
            val progress = CopyOnWriteArrayList<Float>()
            val results = CopyOnWriteArrayList<SceytResponse<String>>()

            utility.uploadFile(attachment(), progress::add, results::add)
            assertThat(uploadStarted.await(2, TimeUnit.SECONDS)).isTrue()

            progressCallbacks.single().onResult(0.4f)
            progressCallbacks.single().onError(SceytException(1, "failed"))
            resultCallbacks.single().onError(SceytException(1, "failed"))

            assertThat(progress).containsExactly(40f)
            assertThat(results).hasSize(1)
            assertThat(results.single()).isInstanceOf(SceytResponse.Error::class.java)
        }
    }

    @Test
    fun `download methods delegate to downloader`() {
        Mockito.mockConstruction(OkHttpDownloader::class.java).use { construction ->
            val utility = FileTransferUtility()
            val downloader = construction.constructed().single()
            val attachment = attachment()
            val destination = File("/tmp/download.txt")
            val onProgress: (Float) -> Unit = {}
            val onResult: (SceytResponse<String>) -> Unit = {}
            whenever(downloader.resumeDownload(attachment)).thenReturn(true)

            utility.downloadFile(attachment, destination, onProgress, onResult)
            utility.pauseDownload(attachment)

            verify(downloader).downloadFile(attachment, destination, onProgress, onResult)
            verify(downloader).pauseDownload(attachment)
            assertThat(utility.resumeDownload(attachment)).isTrue()
            assertThat(utility.resumeUpload(attachment)).isFalse()
        }
    }

    private fun withChatClient(client: ChatClient, block: () -> Unit) {
        val clientField = ChatClient::class.java.getDeclaredField("client").apply {
            isAccessible = true
        }
        val previousClient = clientField.get(null)
        clientField.set(null, client)
        try {
            block()
        } finally {
            clientField.set(null, previousClient)
        }
    }
}
