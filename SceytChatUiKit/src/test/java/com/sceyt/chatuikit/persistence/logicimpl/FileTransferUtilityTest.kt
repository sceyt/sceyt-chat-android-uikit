package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chat.UploadOperation
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FileTransferUtilityTest {

    @Test
    fun `cancelled upload does not affect a new upload for the same attachment`() = runTest {
        val client = mock<ChatClient>()
        val callbacks = mutableListOf<UrlCallback>()
        val operations = mutableListOf<UploadOperation>()
        doAnswer { invocation ->
            callbacks += invocation.getArgument<UrlCallback>(2)
            val operation = mock<UploadOperation>()
            operations += operation
            operation
        }.whenever(client).uploadCancellable(any(), any(), any())

        withChatClient(client) {
            val utility = FileTransferUtility()
            val attachment = attachment(messageTid = 20L)
            val firstUpload = async { utility.uploadFile(attachment, {}) }
            runCurrent()
            firstUpload.cancelAndJoin()

            val secondUpload = async { utility.uploadFile(attachment, {}) }
            runCurrent()
            callbacks.first().onResult("late-result")

            assertThat(secondUpload.isActive).isTrue()
            verify(operations.first()).cancel()
            verify(operations.last(), never()).cancel()

            callbacks.last().onResult("new-result")
            assertThat((secondUpload.await() as SceytResponse.Success).data)
                .isEqualTo("new-result")
        }
    }

    @Test
    fun `cancelled upload cancels SDK handle and ignores late callbacks`() = runTest {
        val client = mock<ChatClient>()
        val operation = mock<UploadOperation>()
        lateinit var progressCallback: ProgressCallback
        lateinit var resultCallback: UrlCallback
        doAnswer { invocation ->
            progressCallback = invocation.getArgument(1)
            resultCallback = invocation.getArgument(2)
            operation
        }.whenever(client).uploadCancellable(any(), any(), any())

        withChatClient(client) {
            val utility = FileTransferUtility()
            val progress = mutableListOf<Float>()

            val upload = async { utility.uploadFile(attachment(), progress::add) }
            runCurrent()
            progressCallback.onResult(0.4f)

            upload.cancelAndJoin()
            verify(operation).cancel()
            progressCallback.onResult(0.8f)
            resultCallback.onResult("late-result")
            resultCallback.onError(SceytException(1, "late failure"))

            assertThat(upload.isCancelled).isTrue()
            assertThat(progress).containsExactly(40f)
        }
    }

    @Test
    fun `sdk error callbacks complete upload once`() = runTest {
        val client = mock<ChatClient>()
        val operation = mock<UploadOperation>()
        lateinit var progressCallback: ProgressCallback
        lateinit var resultCallback: UrlCallback
        doAnswer { invocation ->
            progressCallback = invocation.getArgument(1)
            resultCallback = invocation.getArgument(2)
            operation
        }.whenever(client).uploadCancellable(any(), any(), any())

        withChatClient(client) {
            val utility = FileTransferUtility()
            val progress = mutableListOf<Float>()
            val error = SceytException(1, "failed")

            val upload = async { utility.uploadFile(attachment(), progress::add) }
            runCurrent()

            progressCallback.onResult(0.4f)
            progressCallback.onResult(1f)
            progressCallback.onError(error)
            resultCallback.onError(SceytException(1, "duplicate failure"))

            assertThat(progress).containsExactly(40f)
            assertThat((upload.await() as SceytResponse.Error).exception).isSameInstanceAs(error)
        }
    }

    @Test
    fun `cancellation during SDK startup cancels returned handle before caller cleanup`() = runTest {
        val client = mock<ChatClient>()
        val events = mutableListOf<String>()
        val operation = UploadOperation { events += "sdk-cancelled" }
        lateinit var job: Job
        doAnswer {
            job.cancel()
            operation
        }.whenever(client).uploadCancellable(any(), any(), any())

        withChatClient(client) {
            val utility = FileTransferUtility()
            job = launch(start = CoroutineStart.LAZY) {
                try {
                    utility.uploadFile(attachment(), {})
                } finally {
                    events += "caller-cleanup"
                }
            }
            job.start()
            runCurrent()
            job.join()

            assertThat(events).containsExactly("sdk-cancelled", "caller-cleanup").inOrder()
        }
    }

    @Test
    fun `synchronous SDK completion and duplicate result resume once`() = runTest {
        val client = mock<ChatClient>()
        val operation = mock<UploadOperation>()
        doAnswer { invocation ->
            val callback = invocation.getArgument<UrlCallback>(2)
            callback.onResult("uploaded-url")
            callback.onError(SceytException(1, "late failure"))
            operation
        }.whenever(client).uploadCancellable(any(), any(), any())

        withChatClient(client) {
            val response = FileTransferUtility().uploadFile(attachment(), {})

            assertThat((response as SceytResponse.Success).data).isEqualTo("uploaded-url")
            verify(operation, never()).cancel()
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
        }
    }

    private suspend fun withChatClient(client: ChatClient, block: suspend () -> Unit) {
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
