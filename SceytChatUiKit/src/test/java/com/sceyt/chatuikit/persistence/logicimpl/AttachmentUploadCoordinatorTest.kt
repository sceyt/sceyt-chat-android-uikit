package com.sceyt.chatuikit.persistence.logicimpl

import android.app.Application
import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferServiceImpl
import com.sceyt.chatuikit.persistence.file_transfer.ProgressUpdateCallback
import com.sceyt.chatuikit.persistence.file_transfer.ResumePauseCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferResultCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.shared.media_encoder.CompressionListener
import com.sceyt.chatuikit.shared.media_encoder.CustomVideoCompressor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentUploadCoordinatorTest {
    private lateinit var context: Context
    private lateinit var service: FileTransferServiceImpl
    private lateinit var transport: RecordingFileTransferTransport
    private lateinit var attachmentLogic: PersistenceAttachmentLogic
    private lateinit var coordinator: AttachmentUploadCoordinator
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var previousConfig: SceytChatUIKitConfig
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        stopKoin()
        context = RuntimeEnvironment.getApplication()
        transport = RecordingFileTransferTransport()
        testScope = TestScope(UnconfinedTestDispatcher())
        attachmentLogic = mock()
        previousFileTransfer = SceytChatUIKit.fileTransfer
        previousConfig = SceytChatUIKit.config
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@AttachmentUploadCoordinatorTest.transport
        }
        SceytChatUIKit.config = SceytChatUIKitConfig().apply {
            preventDuplicateAttachmentUpload = false
        }
        coordinator = AttachmentUploadCoordinator(context, attachmentLogic, testScope)
        service = FileTransferServiceImpl(context, coordinator, mock(), mock())
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<FileTransferService> { service }
                single<PersistenceAttachmentLogic> { attachmentLogic }
                single<FileChecksumDao> { mock() }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) { Dispatchers.Unconfined }
            })
        }
    }

    @After
    fun tearDown() {
        service.cancelAllTransfers()
        SceytChatUIKit.fileTransfer = previousFileTransfer
        SceytChatUIKit.config = previousConfig
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `regular uploads run sequentially`() {
        val first = uploadAttachment(messageTid = 1L)
        val second = uploadAttachment(messageTid = 2L)

        uploadFile(first, transferTask(first))
        uploadFile(second, transferTask(second))

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(transport.uploadCalls.single().request.operationId).isEqualTo("upload:1")

        transport.uploadCalls.first().succeed("first-url")

        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls[1].request.operationId).isEqualTo("upload:2")
    }

    @Test
    fun `queued pause before worker admission never starts the upload`() {
        testScope = TestScope(StandardTestDispatcher())
        coordinator = AttachmentUploadCoordinator(context, attachmentLogic, testScope)
        service = FileTransferServiceImpl(context, coordinator, mock(), mock())

        repeat(8) { index ->
            val shared = index % 2 == 0
            val state = if (index % 4 < 2) TransferState.WaitingToUpload else TransferState.PendingUpload
            val attachment = uploadAttachment(messageTid = 220L + index, state = state)
            val task = transferTask(attachment)
            val completions = mutableListOf<Result<SceytAttachment>>()
            var resultCalls = 0
            task.uploadResultCallback = TransferResultCallback { resultCalls++ }

            service.pause(attachment.messageTid, attachment, state)
            val configure: TransferTask.() -> Unit = {
                addOnCompletionListener("worker", completions::add)
            }
            if (shared) service.uploadSharedFile(attachment, configure)
            else service.upload(attachment, configure)
            testScope.runCurrent()

            assertThat(transport.uploadCalls).isEmpty()
            assertThat(completions).hasSize(1)
            assertThat(completions.single().exceptionOrNull()?.message).isEqualTo("Attachment upload is paused")
            assertThat(task.state).isEqualTo(TransferState.PauseUpload)
            assertThat(resultCalls).isEqualTo(0)
            assertThat(service.findTransferTask(attachment)).isSameInstanceAs(task)
            service.cancelUpload(attachment)
            testScope.runCurrent()
        }
    }

    @Test
    fun `resume waits for original cleanup when an upload replacement never starts`() {
        val queued = java.util.ArrayDeque<Runnable>()
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                queued.addLast(block)
            }
        }
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        coordinator = AttachmentUploadCoordinator(context, attachmentLogic, scope)
        service = FileTransferServiceImpl(context, coordinator, mock(), mock())
        val attachment = uploadAttachment(messageTid = 221L)
        val gate = CompletableDeferred<Unit>()
        transport.uploadCancellationGate = gate
        fun drain() {
            while (queued.isNotEmpty()) queued.removeFirst().run()
        }

        try {
            uploadFile(attachment, transferTask(attachment))
            drain()
            pauseUpload(attachment, TransferState.Uploading)
            drain()
            assertThat(transport.uploadCalls.single().cancelled).isTrue()

            resumeUpload(attachment, TransferState.PauseUpload)
            queued.removeFirst().run()
            // Let pause run before the newly dispatched upload gets a thread.
            pauseUpload(attachment, TransferState.WaitingToUpload)
            queued.removeLast().run()
            drain()
            resumeUpload(attachment, TransferState.PauseUpload)
            drain()

            assertThat(transport.uploadCalls).hasSize(1)
            gate.complete(Unit)
            drain()
            assertThat(transport.uploadCalls).hasSize(2)
            transport.uploadCalls.last().succeed("resumed-url")
            drain()
            assertThat(service.findTransferTask(attachment)).isNull()
        } finally {
            gate.complete(Unit)
            scope.cancel()
            drain()
        }
    }

    @Test
    fun `failed concurrent share does not fail or delay the normal upload queue`() {
        val main = uploadAttachment(messageTid = 201L)
        val next = uploadAttachment(messageTid = 202L)
        val concurrent = uploadAttachment(messageTid = 203L)
        val results = mutableListOf<String?>()
        uploadFile(main, transferTask(main).apply {
            uploadResultCallback = TransferResultCallback { results += it.data }
        })
        uploadFile(next, transferTask(next))
        uploadSharedFile(concurrent, transferTask(concurrent))

        assertThat(transport.uploadCalls.map { it.request.operationId })
            .containsExactly("upload:201", "upload:203").inOrder()
        transport.uploadCalls.last().fail(IOException("concurrent upload failed"))
        assertThat(results).isEmpty()
        transport.uploadCalls.first().succeed("main-url")

        assertThat(results).containsExactly("main-url")
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:202")
        transport.uploadCalls.last().succeed("next-url")
    }

    @Test
    fun `paused native upload finishing while requeued does not take another uploads turn`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val paused = uploadAttachment(messageTid = 204L)
        val current = uploadAttachment(messageTid = 205L)
        val next = uploadAttachment(messageTid = 206L)
        uploadFile(paused, transferTask(paused))
        pauseUpload(paused, TransferState.Uploading)
        uploadFile(current, transferTask(current))
        resumeUpload(paused, TransferState.PauseUpload)

        transport.uploadCalls.first().fail(IOException("failed while natively paused"))
        transport.uploadCalls[1].succeed("current-url")
        uploadFile(next, transferTask(next))

        assertThat(transport.uploadCalls.map { it.request.operationId })
            .containsExactly("upload:204", "upload:205", "upload:206").inOrder()
        transport.uploadCalls.last().succeed("next-url")
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `cancel queued upload removes it without stopping current upload`() {
        val first = uploadAttachment(messageTid = 130L)
        val queued = uploadAttachment(messageTid = 131L)
        val next = uploadAttachment(messageTid = 132L)
        uploadFile(first, transferTask(first))
        uploadFile(queued, transferTask(queued))
        uploadFile(next, transferTask(next))

        cancelUpload(queued)
        assertThat(transport.uploadCalls.single().cancelled).isFalse()
        transport.uploadCalls.first().succeed("first-url")

        assertThat(transport.uploadCalls.map { it.request.operationId })
            .containsExactly("upload:130", "upload:132").inOrder()
    }

    @Test
    fun `cancel active upload cancels job advances queue and ignores late callbacks`() {
        transport.pauseResult = true
        val first = uploadAttachment(messageTid = 133L)
        val next = uploadAttachment(messageTid = 134L)
        val progress = mutableListOf<Float>()
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(first).apply {
            progressCallback = ProgressUpdateCallback { progress += it.progressPercent }
            uploadResultCallback = TransferResultCallback { results += it }
        }
        uploadFile(first, task)
        uploadFile(next, transferTask(next))
        val call = transport.uploadCalls.single()

        cancelUpload(first)
        call.progress(50f)
        call.fail(IOException("late cancellation error"))

        assertThat(call.cancelled).isTrue()
        assertThat(transport.pauseCalls).isEmpty()
        assertThat(progress).isEmpty()
        assertThat(results).isEmpty()
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:134")
    }

    @Test
    fun `cancel natively paused upload does not disturb the next upload`() {
        transport.pauseResult = true
        val first = uploadAttachment(messageTid = 135L)
        val next = uploadAttachment(messageTid = 136L)
        val task = transferTask(first)
        uploadFile(first, task)
        uploadFile(next, transferTask(next))
        val call = transport.uploadCalls.single()
        pauseUpload(first, TransferState.Uploading)
        assertThat(call.cancelled).isFalse()

        cancelUpload(first)

        assertThat(call.cancelled).isTrue()
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.last().cancelled).isFalse()
    }

    @Test
    fun `cancel before upload starts does not register a task or leave a pause`() {
        val attachment = uploadAttachment(messageTid = 137L)

        cancelUpload(attachment)

        assertThat(service.getTasks()).isEmpty()
        uploadFile(attachment, transferTask(attachment))
        assertThat(transport.uploadCalls).hasSize(1)
    }

    @Test
    fun `cancel during preparation prevents upload and advances queue`() {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        val first = uploadAttachment(messageTid = 138L)
        val next = uploadAttachment(messageTid = 139L)
        val releaseChecksum = CompletableDeferred<Unit>()
        doSuspendableAnswer {
            releaseChecksum.await()
            null
        }.whenever(attachmentLogic) { getFileChecksumData(first.originalFilePath) }
        uploadFile(first, transferTask(first))
        uploadFile(next, transferTask(next))

        cancelUpload(first)
        releaseChecksum.complete(Unit)

        assertThat(transport.uploadCalls.single().request.operationId).isEqualTo("upload:139")
    }

    @Test
    fun `cancel shared owner keeps follower upload and cleans up owner after result`() {
        val owner = uploadAttachment(messageTid = 140L)
        val follower = uploadAttachment(messageTid = 141L, filePath = owner.filePath)
        val ownerProgress = mutableListOf<Float>()
        val ownerResults = mutableListOf<String?>()
        val followerResults = mutableListOf<String?>()
        uploadSharedFile(owner, transferTask(owner).apply {
            progressCallback = ProgressUpdateCallback { ownerProgress += it.progressPercent }
            uploadResultCallback = TransferResultCallback { ownerResults += it.data }
        })
        uploadSharedFile(follower, transferTask(follower).apply {
            uploadResultCallback = TransferResultCallback { followerResults += it.data }
        })
        val call = transport.uploadCalls.single()

        cancelUpload(owner)
        assertThat(call.cancelled).isFalse()
        call.progress(50f)
        call.succeed("shared-url")

        assertThat(ownerProgress).isEmpty()
        assertThat(ownerResults).isEmpty()
        assertThat(followerResults).containsExactly("shared-url")
        uploadSharedFile(owner, transferTask(owner))
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `cancel shared owner during preparation preserves follower without updating deleted task`() {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        val owner = uploadAttachment(messageTid = 148L)
        val follower = uploadAttachment(messageTid = 149L, filePath = owner.filePath)
        val releaseChecksum = CompletableDeferred<Unit>()
        val ownerPaths = mutableListOf<String>()
        val followerResults = mutableListOf<String?>()
        doSuspendableAnswer {
            releaseChecksum.await()
            FileChecksumData(1L, owner.filePath, null, null, owner.fileSize)
        }.whenever(attachmentLogic) { getFileChecksumData(owner.originalFilePath) }
        uploadSharedFile(owner, transferTask(owner).apply {
            updateFileLocationCallback = { ownerPaths += it }
        })
        uploadSharedFile(follower, transferTask(follower).apply {
            uploadResultCallback = TransferResultCallback { followerResults += it.data }
        })

        cancelUpload(owner)
        releaseChecksum.complete(Unit)
        transport.uploadCalls.single().succeed("shared-url")

        assertThat(ownerPaths).isEmpty()
        assertThat(followerResults).containsExactly("shared-url")
    }

    @Test
    fun `cancel shared owner during interruption cleanup still delivers follower error`() {
        val releaseCancellation = CompletableDeferred<Unit>()
        SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
            override suspend fun upload(request: FileUploadRequest, callback: FileTransferCallback): FileTransferResult<String> {
                try {
                    return transport.upload(request, callback)
                } finally {
                    withContext(NonCancellable) { releaseCancellation.await() }
                }
            }
        }
        val owner = uploadAttachment(messageTid = 150L)
        val follower = uploadAttachment(messageTid = 151L, filePath = owner.filePath)
        val followerResults = mutableListOf<SceytResponse<String>>()
        uploadSharedFile(owner, transferTask(owner))
        uploadSharedFile(follower, transferTask(follower).apply {
            uploadResultCallback = TransferResultCallback { followerResults += it }
        })

        transport.uploadCalls.single().interrupt()
        cancelUpload(owner)
        releaseCancellation.complete(Unit)

        assertThat(followerResults.single().message).isEqualTo("Network interrupted")
        assertThat(transport.uploadCalls.single().cancelled).isFalse()
        uploadSharedFile(owner, transferTask(owner))
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `cancel shared follower leaves owner running`() {
        val owner = uploadAttachment(messageTid = 142L)
        val follower = uploadAttachment(messageTid = 143L, filePath = owner.filePath)
        val ownerResults = mutableListOf<String?>()
        val followerResults = mutableListOf<String?>()
        uploadSharedFile(owner, transferTask(owner).apply {
            uploadResultCallback = TransferResultCallback { ownerResults += it.data }
        })
        uploadSharedFile(follower, transferTask(follower).apply {
            uploadResultCallback = TransferResultCallback { followerResults += it.data }
        })
        val call = transport.uploadCalls.single()

        cancelUpload(follower)
        assertThat(call.cancelled).isFalse()
        call.succeed("shared-url")

        assertThat(ownerResults).containsExactly("shared-url")
        assertThat(followerResults).isEmpty()
    }

    @Test
    fun `remaining shared member can pause resume and cancel after owner deletion`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val owner = uploadAttachment(messageTid = 144L)
        val follower = uploadAttachment(messageTid = 145L, filePath = owner.filePath)
        uploadSharedFile(owner, transferTask(owner))
        uploadSharedFile(follower, transferTask(follower))
        val call = transport.uploadCalls.single()

        cancelUpload(owner)
        pauseUpload(follower, TransferState.Uploading)
        resumeUpload(follower, TransferState.PauseUpload)

        assertThat(transport.pauseCalls).containsExactly("upload:144")
        assertThat(transport.resumeCalls).containsExactly("upload:144")
        assertThat(transport.uploadCalls).hasSize(1)
        cancelUpload(follower)
        assertThat(call.cancelled).isTrue()
        assertThat(service.getTasks()).isEmpty()
        uploadSharedFile(owner, transferTask(owner))
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `deleting last active shared member retains native transfer for paused member`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val owner = uploadAttachment(messageTid = 146L)
        val follower = uploadAttachment(messageTid = 147L, filePath = owner.filePath)
        val followerResults = mutableListOf<String?>()
        uploadSharedFile(owner, transferTask(owner))
        uploadSharedFile(follower, transferTask(follower).apply {
            uploadResultCallback = TransferResultCallback { followerResults += it.data }
        })
        val call = transport.uploadCalls.single()
        pauseUpload(follower, TransferState.Uploading)

        cancelUpload(owner)

        assertThat(call.cancelled).isFalse()
        assertThat(transport.pauseCalls).containsExactly("upload:146")
        resumeUpload(follower, TransferState.PauseUpload)
        assertThat(transport.resumeCalls).containsExactly("upload:146")
        assertThat(transport.uploadCalls).hasSize(1)
        call.succeed("resumed-url")
        assertThat(followerResults).containsExactly("resumed-url")
    }

    @Test
    fun `deleting a member does not pause an already paused native transfer again`() {
        transport.pauseResult = true
        val owner = uploadAttachment(messageTid = 303L)
        val follower = uploadAttachment(messageTid = 304L, filePath = owner.filePath)
        uploadSharedFile(owner, transferTask(owner))
        uploadSharedFile(follower, transferTask(follower))
        val call = transport.uploadCalls.single()
        pauseUpload(owner, TransferState.Uploading)
        pauseUpload(follower, TransferState.Uploading)
        transport.pauseResult = false

        cancelUpload(owner)

        assertThat(call.cancelled).isFalse()
        assertThat(transport.pauseCalls).containsExactly("upload:303")
        cancelUpload(follower)
        assertThat(call.cancelled).isTrue()
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `deleting last active shared member restarts on resume when native pause is unsupported`() {
        transport.pauseResult = false
        val owner = uploadAttachment(messageTid = 305L)
        val follower = uploadAttachment(messageTid = 306L, filePath = owner.filePath)
        val results = mutableListOf<String?>()
        uploadSharedFile(owner, transferTask(owner))
        uploadSharedFile(follower, transferTask(follower).apply {
            uploadResultCallback = TransferResultCallback { results += it.data }
        })
        val call = transport.uploadCalls.single()
        pauseUpload(follower, TransferState.Uploading)

        cancelUpload(owner)

        assertThat(call.cancelled).isTrue()
        assertThat(transport.pauseCalls).containsExactly("upload:305")
        resumeUpload(follower, TransferState.PauseUpload)
        assertThat(transport.resumeCalls).isEmpty()
        assertThat(transport.uploadCalls).hasSize(2)
        transport.uploadCalls.last().succeed("restarted-url")
        assertThat(results).containsExactly("restarted-url")
    }

    @Test
    fun `duplicate queued upload is suppressed`() {
        val current = uploadAttachment(messageTid = 1L)
        val duplicate = uploadAttachment(messageTid = 2L)

        uploadFile(current, transferTask(current))
        uploadFile(duplicate, transferTask(duplicate))
        uploadFile(duplicate, transferTask(duplicate))
        transport.uploadCalls.first().succeed("current-url")

        assertThat(transport.uploadCalls.map { it.request.operationId })
            .containsExactly("upload:1", "upload:2").inOrder()
    }

    @Test
    fun `transport receives prepared request and forwards callbacks`() {
        val attachment = uploadAttachment(
            messageTid = 30L,
            name = "document.txt",
        )
        val task = transferTask(attachment)
        val progress = mutableListOf<TransferData>()
        var result: SceytResponse<String>? = null
        task.progressCallback = ProgressUpdateCallback { progress += it }
        task.uploadResultCallback = TransferResultCallback { result = it }

        uploadFile(attachment, task)
        val call = transport.uploadCalls.single()
        call.progress(55f)
        call.succeed("uploaded-url")

        assertThat(call.request.operationId).isEqualTo("upload:30")
        assertThat(call.request.sourceFile.path).isEqualTo(attachment.filePath)
        assertThat(call.request.fileName).isEqualTo("document.txt")
        assertThat(call.request.context).isEqualTo(AttachmentTransferContext(attachment))
        assertThat(progress.single().progressPercent).isEqualTo(55f)
        assertThat(progress.single().state).isEqualTo(TransferState.Uploading)
        assertThat(result?.data).isEqualTo("uploaded-url")
    }

    @Test
    fun `existing checksum url skips transport`() = runBlocking {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        val attachment = uploadAttachment()
        val task = transferTask(attachment)
        var result: SceytResponse<String>? = null
        task.uploadResultCallback = TransferResultCallback { result = it }
        whenever(attachmentLogic.getFileChecksumData(attachment.originalFilePath)).thenReturn(
            FileChecksumData(
                checksum = 1L,
                resizedFilePath = null,
                url = "reused-url",
                metadata = null,
                fileSize = 4L,
            ),
        )

        uploadFile(attachment, task)

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(result?.data).isEqualTo("reused-url")
    }

    @Test
    fun `shared attachments use one physical upload and fan out callbacks`() {
        val first = uploadAttachment(messageTid = 1L)
        val second = uploadAttachment(messageTid = 2L, filePath = first.filePath)
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        val firstProgress = mutableListOf<Float>()
        val secondProgress = mutableListOf<Float>()
        val firstResults = mutableListOf<String?>()
        val secondResults = mutableListOf<String?>()
        firstTask.progressCallback = ProgressUpdateCallback { firstProgress += it.progressPercent }
        secondTask.progressCallback = ProgressUpdateCallback { secondProgress += it.progressPercent }
        firstTask.uploadResultCallback = TransferResultCallback { firstResults += it.data }
        secondTask.uploadResultCallback = TransferResultCallback { secondResults += it.data }

        uploadSharedFile(first, firstTask)
        uploadSharedFile(second, secondTask)
        val call = transport.uploadCalls.single()
        call.progress(60f)
        call.succeed("shared-url")

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(firstProgress).containsExactly(60f)
        assertThat(secondProgress).containsExactly(60f)
        assertThat(firstResults).containsExactly("shared-url")
        assertThat(secondResults).containsExactly("shared-url")
    }

    @Test
    fun `shared upload pauses physically only after every task is paused`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val first = uploadAttachment(messageTid = 3L)
        val second = uploadAttachment(messageTid = 4L, filePath = first.filePath)
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        val firstProgress = mutableListOf<Float>()
        val secondProgress = mutableListOf<Float>()
        firstTask.progressCallback = ProgressUpdateCallback { firstProgress += it.progressPercent }
        secondTask.progressCallback = ProgressUpdateCallback { secondProgress += it.progressPercent }

        uploadSharedFile(first, firstTask)
        uploadSharedFile(second, secondTask)
        val call = transport.uploadCalls.single()

        pauseUpload(first, TransferState.Uploading)
        call.progress(25f)

        assertThat(transport.pauseCalls).isEmpty()
        assertThat(firstProgress).isEmpty()
        assertThat(secondProgress).containsExactly(25f)

        pauseUpload(second, TransferState.Uploading)

        assertThat(transport.pauseCalls).containsExactly("upload:3")
        assertThat(call.cancelled).isFalse()

        resumeUpload(second, TransferState.PauseUpload)
        call.progress(50f)

        assertThat(transport.resumeCalls).containsExactly("upload:3")
        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(firstProgress).isEmpty()
        assertThat(secondProgress).containsExactly(25f, 50f).inOrder()

        call.succeed("shared-url")
    }

    @Test
    fun `paused shared owner still uploads for an active follower`() {
        val scope = TestScope(StandardTestDispatcher())
        coordinator = AttachmentUploadCoordinator(context, attachmentLogic, scope)
        service = FileTransferServiceImpl(context, coordinator, mock(), mock())
        val first = uploadAttachment(messageTid = 80L, state = TransferState.Uploading)
        val second = uploadAttachment(
            messageTid = 81L,
            filePath = first.filePath,
            state = TransferState.Uploading,
        )

        uploadSharedFile(first, transferTask(first))
        uploadSharedFile(second, transferTask(second))
        pauseUpload(first, TransferState.Uploading)
        scope.runCurrent()

        assertThat(transport.uploadCalls.map { it.request.operationId })
            .containsExactly("upload:80")
        assertThat(transport.uploadCalls.single().cancelled).isFalse()
    }

    @Test
    fun `regular upload with shared source is not treated as a shared member`() {
        val shared = uploadAttachment(messageTid = 82L, state = TransferState.Uploading)
        val follower = uploadAttachment(
            messageTid = 83L,
            filePath = shared.filePath,
            state = TransferState.Uploading,
        )
        val regular = uploadAttachment(
            messageTid = 84L,
            filePath = shared.filePath,
            state = TransferState.Uploading,
        )

        uploadSharedFile(shared, transferTask(shared))
        uploadSharedFile(follower, transferTask(follower))
        uploadFile(regular, transferTask(regular))
        pauseUpload(regular, TransferState.Uploading)

        assertThat(transport.pauseCalls).containsExactly("upload:84")
        assertThat(transport.uploadCalls.first().cancelled).isFalse()
        assertThat(transport.uploadCalls.last().cancelled).isTrue()
    }

    @Test
    fun `shared upload falls back to file path when original path is missing`() {
        val first = uploadAttachment(messageTid = 85L).copy(originalFilePath = null)
        val second = uploadAttachment(
            messageTid = 86L,
            filePath = first.filePath,
        ).copy(originalFilePath = null)

        uploadSharedFile(first, transferTask(first))
        uploadSharedFile(second, transferTask(second))

        assertThat(transport.uploadCalls).hasSize(1)
    }

    @Test
    fun `paused shared member reuses result completed by active member`() {
        val first = uploadAttachment(messageTid = 87L, state = TransferState.Uploading)
        val second = uploadAttachment(
            messageTid = 88L,
            filePath = first.filePath,
            state = TransferState.Uploading,
        )
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        val firstResults = mutableListOf<String?>()
        firstTask.uploadResultCallback = TransferResultCallback { firstResults += it.data }
        secondTask.uploadResultCallback = TransferResultCallback { response ->
            response.data?.let { url ->
                secondTask.attachment = secondTask.attachment.copy(
                    url = url,
                    transferState = TransferState.Uploaded,
                )
                secondTask.state = TransferState.Uploaded
                assertThat(service.findTransferTask(second)).isNull()
            }
        }

        uploadSharedFile(first, firstTask)
        uploadSharedFile(second, secondTask)
        pauseUpload(first, TransferState.Uploading)
        transport.uploadCalls.single().succeed("shared-url")

        uploadFile(first, firstTask)
        assertThat(transport.uploadCalls).hasSize(1)
        resumeUpload(first, TransferState.PauseUpload)

        assertThat(firstResults).containsExactly("shared-url")
        assertThat(transport.uploadCalls).hasSize(1)
    }

    @Test
    fun `new share starts after group completes with a paused member`() {
        val first = uploadAttachment(messageTid = 94L)
        val paused = uploadAttachment(messageTid = 95L, filePath = first.filePath)
        val next = uploadAttachment(messageTid = 96L, filePath = first.filePath)
        val pausedResults = mutableListOf<String?>()
        val nextResults = mutableListOf<String?>()
        val pausedTask = transferTask(paused).apply {
            uploadResultCallback = TransferResultCallback { pausedResults += it.data }
        }
        val nextTask = transferTask(next).apply {
            uploadResultCallback = TransferResultCallback { nextResults += it.data }
        }

        uploadSharedFile(first, transferTask(first))
        uploadSharedFile(paused, pausedTask)
        pauseUpload(paused, TransferState.Uploading)
        transport.uploadCalls.single().succeed("first-url")

        uploadSharedFile(next, nextTask)

        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:96")
        assertThat(pausedResults).isEmpty()

        resumeUpload(paused, TransferState.PauseUpload)
        transport.uploadCalls.last().succeed("next-url")

        assertThat(pausedResults).containsExactly("first-url")
        assertThat(nextResults).containsExactly("next-url")
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `new share reuses checksum after group completes with a paused member`() = runBlocking {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        val first = uploadAttachment(messageTid = 97L)
        val paused = uploadAttachment(messageTid = 98L, filePath = first.filePath)
        val next = uploadAttachment(messageTid = 99L, filePath = first.filePath)
        val results = mutableListOf<String?>()
        val pausedResults = mutableListOf<String?>()
        val pausedTask = transferTask(paused).apply {
            uploadResultCallback = TransferResultCallback { pausedResults += it.data }
        }
        val nextTask = transferTask(next).apply {
            uploadResultCallback = TransferResultCallback { results += it.data }
        }

        uploadSharedFile(first, transferTask(first))
        uploadSharedFile(paused, pausedTask)
        pauseUpload(paused, TransferState.Uploading)
        transport.uploadCalls.single().succeed("first-url")
        whenever(attachmentLogic.getFileChecksumData(first.originalFilePath)).thenReturn(
            FileChecksumData(
                checksum = 1L,
                resizedFilePath = null,
                url = "cached-url",
                metadata = null,
                fileSize = 4L,
            ),
        )

        uploadSharedFile(next, nextTask)

        assertThat(results).containsExactly("cached-url")
        assertThat(pausedResults).isEmpty()
        resumeUpload(paused, TransferState.PauseUpload)
        assertThat(pausedResults).containsExactly("first-url")
        assertThat(transport.uploadCalls).hasSize(1)
    }

    @Test
    fun `new share starts after group fails with a paused member`() {
        val first = uploadAttachment(messageTid = 100L)
        val paused = uploadAttachment(messageTid = 101L, filePath = first.filePath)
        val next = uploadAttachment(messageTid = 102L, filePath = first.filePath)
        val firstResults = mutableListOf<SceytResponse<String>>()
        val pausedResults = mutableListOf<String?>()
        val nextResults = mutableListOf<String?>()
        val pausedTask = transferTask(paused).apply {
            uploadResultCallback = TransferResultCallback { pausedResults += it.data }
        }

        uploadSharedFile(first, transferTask(first).apply {
            uploadResultCallback = TransferResultCallback { firstResults += it }
        })
        uploadSharedFile(paused, pausedTask)
        pauseUpload(paused, TransferState.Uploading)
        transport.uploadCalls.single().fail(IllegalStateException("upload failed"))

        uploadFile(paused, pausedTask)
        assertThat(transport.uploadCalls).hasSize(1)
        uploadSharedFile(next, transferTask(next).apply {
            uploadResultCallback = TransferResultCallback { nextResults += it.data }
        })

        assertThat(firstResults.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(pausedResults).isEmpty()
        assertThat(transport.uploadCalls).hasSize(2)
        transport.uploadCalls.last().succeed("next-url")
        assertThat(nextResults).containsExactly("next-url")

        resumeUpload(paused, TransferState.PauseUpload)
        assertThat(pausedResults).containsExactly("next-url")
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `checksum shared completion retains result for paused member and allows next share`() = runBlocking {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        testScope = TestScope(StandardTestDispatcher())
        coordinator = AttachmentUploadCoordinator(context, attachmentLogic, testScope)
        service = FileTransferServiceImpl(context, coordinator, mock(), mock())
        val first = uploadAttachment(messageTid = 103L)
        val paused = uploadAttachment(messageTid = 104L, filePath = first.filePath)
        val next = uploadAttachment(messageTid = 105L, filePath = first.filePath)
        val firstResults = mutableListOf<String?>()
        val pausedResults = mutableListOf<String?>()
        val nextResults = mutableListOf<String?>()
        val pausedTask = transferTask(paused).apply {
            uploadResultCallback = TransferResultCallback { pausedResults += it.data }
        }
        whenever(attachmentLogic.getFileChecksumData(first.originalFilePath)).thenReturn(
            FileChecksumData(
                checksum = 1L,
                resizedFilePath = null,
                url = "cached-url",
                metadata = null,
                fileSize = 4L,
            ),
        )

        uploadSharedFile(first, transferTask(first).apply {
            uploadResultCallback = TransferResultCallback { firstResults += it.data }
        })
        uploadSharedFile(paused, pausedTask)
        pauseUpload(paused, TransferState.PendingUpload)
        testScope.runCurrent()

        uploadSharedFile(next, transferTask(next).apply {
            uploadResultCallback = TransferResultCallback { nextResults += it.data }
        })
        testScope.runCurrent()

        assertThat(firstResults).containsExactly("cached-url")
        assertThat(nextResults).containsExactly("cached-url")
        assertThat(pausedResults).isEmpty()
        resumeUpload(paused, TransferState.PauseUpload)
        testScope.runCurrent()
        assertThat(pausedResults).containsExactly("cached-url")
        Mockito.verify(attachmentLogic, Mockito.times(2))
            .getFileChecksumData(first.originalFilePath)
        assertThat(transport.uploadCalls).isEmpty()
    }

    @Test
    fun `new share resumes natively paused group without another upload`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val first = uploadAttachment(messageTid = 106L)
        val paused = uploadAttachment(messageTid = 107L, filePath = first.filePath)
        val next = uploadAttachment(messageTid = 108L, filePath = first.filePath)
        val firstResults = mutableListOf<String?>()
        val pausedResults = mutableListOf<String?>()
        val nextResults = mutableListOf<String?>()
        val firstTask = transferTask(first).apply {
            uploadResultCallback = TransferResultCallback { firstResults += it.data }
        }
        val pausedTask = transferTask(paused).apply {
            uploadResultCallback = TransferResultCallback { pausedResults += it.data }
        }

        uploadSharedFile(first, firstTask)
        uploadSharedFile(paused, pausedTask)
        pauseUpload(first, TransferState.Uploading)
        pauseUpload(paused, TransferState.Uploading)
        uploadSharedFile(next, transferTask(next).apply {
            uploadResultCallback = TransferResultCallback { nextResults += it.data }
        })

        assertThat(transport.pauseCalls).containsExactly("upload:106")
        assertThat(transport.resumeCalls).containsExactly("upload:106")
        assertThat(transport.uploadCalls).hasSize(1)
        transport.uploadCalls.single().succeed("shared-url")
        assertThat(nextResults).containsExactly("shared-url")
        assertThat(firstResults).isEmpty()
        assertThat(pausedResults).isEmpty()

        resumeUpload(first, TransferState.PauseUpload)
        resumeUpload(paused, TransferState.PauseUpload)
        assertThat(firstResults).containsExactly("shared-url")
        assertThat(pausedResults).containsExactly("shared-url")
        assertThat(transport.uploadCalls).hasSize(1)
    }

    @Test
    fun `new share restarts paused group when native resume is unsupported`() {
        transport.pauseResult = true
        val first = uploadAttachment(messageTid = 109L)
        val next = uploadAttachment(messageTid = 110L, filePath = first.filePath)
        val firstResults = mutableListOf<String?>()
        val nextResults = mutableListOf<String?>()
        val firstTask = transferTask(first).apply {
            uploadResultCallback = TransferResultCallback { firstResults += it.data }
        }

        uploadSharedFile(first, firstTask)
        val firstCall = transport.uploadCalls.single()
        pauseUpload(first, TransferState.Uploading)
        uploadSharedFile(next, transferTask(next).apply {
            uploadResultCallback = TransferResultCallback { nextResults += it.data }
        })

        assertThat(transport.resumeCalls).containsExactly("upload:109")
        assertThat(firstCall.cancelled).isTrue()
        assertThat(transport.uploadCalls).hasSize(2)
        transport.uploadCalls.last().succeed("next-url")
        assertThat(nextResults).containsExactly("next-url")
        assertThat(firstResults).isEmpty()

        resumeUpload(first, TransferState.PauseUpload)
        assertThat(firstResults).containsExactly("next-url")
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `share started from completion callback does not resume completed owner`() {
        transport.resumeResult = true
        val first = uploadAttachment(messageTid = 111L)
        val follower = uploadAttachment(messageTid = 112L, filePath = first.filePath)
        val next = uploadAttachment(messageTid = 113L, filePath = first.filePath)
        val firstResults = mutableListOf<String?>()
        val nextResults = mutableListOf<String?>()
        val firstTask = transferTask(first).apply {
            uploadResultCallback = TransferResultCallback { firstResults += it.data }
        }
        val nextTask = transferTask(next).apply {
            uploadResultCallback = TransferResultCallback { nextResults += it.data }
        }
        val followerTask = transferTask(follower).apply {
            uploadResultCallback = TransferResultCallback {
                uploadSharedFile(next, nextTask)
            }
        }

        uploadSharedFile(first, firstTask)
        uploadSharedFile(follower, followerTask)
        pauseUpload(first, TransferState.Uploading)
        transport.uploadCalls.single().succeed("first-url")

        assertThat(transport.resumeCalls).isEmpty()
        assertThat(transport.uploadCalls).hasSize(2)
        transport.uploadCalls.last().succeed("next-url")
        assertThat(nextResults).containsExactly("next-url")
        resumeUpload(first, TransferState.PauseUpload)
        assertThat(firstResults).containsExactly("first-url")
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `native upload resume waits for its queue turn`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val first = uploadAttachment(messageTid = 5L, state = TransferState.Uploading)
        val second = uploadAttachment(messageTid = 6L)
        val firstTask = transferTask(first)

        uploadFile(first, firstTask)
        uploadFile(second, transferTask(second))
        val firstCall = transport.uploadCalls.single()

        pauseUpload(first, TransferState.Uploading)

        assertThat(transport.pauseCalls).containsExactly("upload:5")
        assertThat(firstCall.cancelled).isFalse()
        assertThat(transport.uploadCalls.map { it.request.operationId })
            .containsExactly("upload:5", "upload:6").inOrder()

        resumeUpload(first, TransferState.PauseUpload)

        assertThat(transport.resumeCalls).isEmpty()

        transport.uploadCalls.last().succeed("second-url")

        assertThat(transport.resumeCalls).containsExactly("upload:5")
        assertThat(transport.uploadCalls).hasSize(2)

        firstCall.succeed("first-url")
    }

    @Test
    fun `failed native upload resume restarts the operation`() {
        transport.pauseResult = true
        transport.resumeResult = false
        val attachment = uploadAttachment(messageTid = 7L, state = TransferState.Uploading)
        val task = transferTask(attachment)

        uploadFile(attachment, task)
        pauseUpload(attachment, TransferState.Uploading)
        resumeUpload(attachment, TransferState.PauseUpload)

        assertThat(transport.resumeCalls).containsExactly("upload:7")
        assertThat(transport.uploadCalls.first().cancelled).isTrue()
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `failed natively paused upload can retry without resumeLoad`() {
        transport.pauseResult = true
        val attachment = uploadAttachment(messageTid = 114L)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            uploadResultCallback = TransferResultCallback {
                results += it
                assertThat(service.findTransferTask(attachment)).isNull()
            }
        }

        uploadFile(attachment, task)
        pauseUpload(attachment, TransferState.Uploading)
        transport.uploadCalls.single().fail(IOException("upload failed"))
        assertThat(results.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(service.findTransferTask(attachment)).isNull()

        val retry = attachment.copy(transferState = TransferState.ErrorUpload)
        val retryTask = service.findOrCreateTransferTask(retry).apply {
            uploadResultCallback = TransferResultCallback { results += it }
        }
        uploadFile(retry, retryTask)

        assertThat(transport.uploadCalls).hasSize(2)
        transport.uploadCalls.last().succeed("retry-url")
        assertThat(results.map { it.data }).containsExactly(null, "retry-url").inOrder()
    }

    @Test
    fun `stale pause during failed upload callback does not block retry`() {
        transport.pauseResult = true
        val attachment = uploadAttachment(messageTid = 121L)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            uploadResultCallback = TransferResultCallback {
                results += it
                assertThat(service.findTransferTask(attachment)).isNull()
                pauseUpload(attachment, TransferState.Uploading)
            }
        }

        uploadFile(attachment, task)
        pauseUpload(attachment, TransferState.Uploading)
        transport.uploadCalls.single().fail(IOException("upload failed"))

        val retry = attachment.copy(transferState = TransferState.ErrorUpload)
        val retryTask = service.findOrCreateTransferTask(retry).apply {
            uploadResultCallback = TransferResultCallback { results += it }
        }
        uploadFile(retry, retryTask)

        assertThat(transport.pauseCalls).containsExactly("upload:121")
        assertThat(transport.uploadCalls).hasSize(2)
        transport.uploadCalls.last().succeed("retry-url")
        assertThat(results.map { it.data }).containsExactly(null, "retry-url").inOrder()
    }

    @Test
    fun `stale pause after failed upload job finishes does not block retry`() {
        transport.pauseResult = true
        val attachment = uploadAttachment(messageTid = 122L)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            uploadResultCallback = TransferResultCallback {
                results += it
                assertThat(service.findTransferTask(attachment)).isNull()
            }
        }

        uploadFile(attachment, task)
        pauseUpload(attachment, TransferState.Uploading)
        transport.uploadCalls.single().fail(IOException("upload failed"))
        pauseUpload(attachment, TransferState.Uploading)

        val retry = attachment.copy(transferState = TransferState.ErrorUpload)
        val retryTask = service.findOrCreateTransferTask(retry).apply {
            uploadResultCallback = TransferResultCallback { results += it }
        }
        uploadFile(retry, retryTask)

        assertThat(transport.uploadCalls).hasSize(2)
        transport.uploadCalls.last().succeed("retry-url")
        assertThat(results.map { it.data }).containsExactly(null, "retry-url").inOrder()
    }

    @Test
    fun `pending and queued uploads can pause before task registration`() {
        listOf(TransferState.PendingUpload, TransferState.WaitingToUpload).forEachIndexed { index, state ->
            val attachment = uploadAttachment(messageTid = 123L + index, state = state)
            pauseUpload(attachment, state)

            val task = service.findOrCreateTransferTask(attachment)
            uploadFile(attachment, task)
            assertThat(transport.uploadCalls).hasSize(index)

            resumeUpload(attachment, TransferState.PauseUpload)
            assertThat(transport.uploadCalls).hasSize(index + 1)
            transport.uploadCalls.last().succeed("uploaded-url")
        }
    }

    @Test
    fun `retry from paused upload failure callback retains its queue turn`() {
        transport.pauseResult = true
        val attachment = uploadAttachment(messageTid = 115L)
        val queued = uploadAttachment(messageTid = 116L)
        val retryResults = mutableListOf<String?>()
        val task = transferTask(attachment).apply {
            uploadResultCallback = TransferResultCallback {
                assertThat(service.findTransferTask(attachment)).isNull()
                val retry = attachment.copy(transferState = TransferState.ErrorUpload)
                val retryTask = service.findOrCreateTransferTask(retry).apply {
                    uploadResultCallback = TransferResultCallback { retryResults += it.data }
                }
                uploadFile(retry, retryTask)
                uploadFile(queued, transferTask(queued))
            }
        }

        uploadFile(attachment, task)
        pauseUpload(attachment, TransferState.Uploading)
        transport.uploadCalls.single().fail(IOException("upload failed"))

        assertThat(transport.uploadCalls.map { it.request.operationId })
            .containsExactly("upload:115", "upload:115").inOrder()
        transport.uploadCalls.last().succeed("retry-url")
        assertThat(retryResults).containsExactly("retry-url")
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:116")
        transport.uploadCalls.last().succeed("queued-url")
    }

    @Test
    fun `late IOException from cancelled upload does not fail or advance its replacement`() {
        val cancellationGate = CompletableDeferred<Unit>()
        SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
            override suspend fun upload(request: FileUploadRequest, callback: FileTransferCallback): FileTransferResult<String> {
                return try {
                    transport.upload(request, callback)
                } catch (_: CancellationException) {
                    withContext(NonCancellable) { cancellationGate.await() }
                    throw IOException("backend cancelled")
                }
            }
        }
        val attachment = uploadAttachment(messageTid = 117L)
        val queued = uploadAttachment(messageTid = 118L)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            uploadResultCallback = TransferResultCallback { results += it }
        }

        uploadFile(attachment, task)
        pauseUpload(attachment, TransferState.Uploading)
        uploadFile(attachment, task)
        assertThat(transport.uploadCalls).hasSize(1)
        resumeUpload(attachment, TransferState.PauseUpload)
        uploadFile(queued, transferTask(queued))
        cancellationGate.complete(Unit)

        assertThat(results).isEmpty()
        assertThat(transport.uploadCalls).hasSize(2)
        transport.uploadCalls.last().succeed("replacement-url")
        assertThat(results.single().data).isEqualTo("replacement-url")
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:118")
        transport.uploadCalls.last().succeed("queued-url")
    }

    @Test
    fun `interruption advances queue without cancelling the backend`() {
        val attachment = uploadAttachment(messageTid = 119L)
        val queued = uploadAttachment(messageTid = 120L)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            uploadResultCallback = TransferResultCallback { results += it }
        }

        uploadFile(attachment, task)
        uploadFile(queued, transferTask(queued))
        transport.uploadCalls.single().interrupt()

        assertThat(results.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(transport.uploadCalls.first().cancelled).isFalse()
        assertThat(service.findTransferTask(attachment)).isNull()
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:120")
        transport.uploadCalls.last().succeed("queued-url")
    }

    @Test
    fun `pause cancels upload and resume requeues it`() {
        val attachment = uploadAttachment(messageTid = 40L, state = TransferState.Uploading)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }

        uploadFile(attachment, task)
        pauseUpload(attachment, TransferState.Uploading)
        resumeUpload(attachment, TransferState.PauseUpload)

        assertThat(transport.uploadCalls.first().cancelled).isTrue()
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(states).containsExactly(
            TransferState.PauseUpload,
            TransferState.WaitingToUpload,
        ).inOrder()
    }

    @Test
    fun `resume publishes the prepared path for normal and shared uploads`() {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        repeat(4) { index ->
            val shared = index % 2 == 0
            val nativePause = index >= 2
            transport.pauseResult = nativePause
            transport.resumeResult = nativePause
            val attachment = uploadAttachment(messageTid = 240L + index)
            val prepared = File(context.cacheDir, "prepared-${attachment.messageTid}.txt")
                .apply { writeText("prepared") }
            doSuspendableAnswer {
                FileChecksumData(1L, prepared.path, null, null, prepared.length())
            }.whenever(attachmentLogic) { getFileChecksumData(attachment.originalFilePath) }
            val updates = mutableListOf<TransferData>()
            val task = transferTask(attachment).apply {
                updateFileLocationCallback = { path ->
                    this.attachment = this.attachment.copy(filePath = path)
                }
                resumePauseCallback = ResumePauseCallback { updates += it }
            }

            if (shared) uploadSharedFile(attachment, task) else uploadFile(attachment, task)
            assertThat(transport.uploadCalls.last().request.sourceFile).isEqualTo(prepared)
            pauseUpload(attachment, TransferState.Uploading)
            resumeUpload(attachment, TransferState.PauseUpload)

            assertThat(updates.map { it.state }).containsExactly(
                TransferState.PauseUpload, TransferState.WaitingToUpload,
            ).inOrder()
            assertThat(updates.map { it.filePath }).containsExactly(prepared.path, prepared.path)
            transport.uploadCalls.last().succeed("uploaded-${attachment.messageTid}")
        }
    }

    @Test
    fun `pausing active upload advances the sequential queue`() {
        val first = uploadAttachment(messageTid = 50L, state = TransferState.WaitingToUpload)
        val second = uploadAttachment(messageTid = 51L, state = TransferState.WaitingToUpload)
        val firstTask = transferTask(first)
        val states = mutableListOf<TransferState>()
        firstTask.resumePauseCallback = ResumePauseCallback { states += it.state }

        uploadFile(first, firstTask)
        uploadFile(second, transferTask(second))
        pauseUpload(first, TransferState.WaitingToUpload)

        assertThat(transport.uploadCalls.first().cancelled).isTrue()
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:51")
        assertThat(firstTask.state).isEqualTo(TransferState.PauseUpload)
        assertThat(states).containsExactly(TransferState.PauseUpload)
    }

    @Test
    fun `duplicate upload request does not resume a paused upload`() {
        val attachment = uploadAttachment(messageTid = 58L, state = TransferState.Uploading)
        val task = transferTask(attachment)

        uploadFile(attachment, task)
        pauseUpload(attachment, TransferState.Uploading)
        uploadFile(attachment, task)

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(transport.uploadCalls.single().cancelled).isTrue()
    }

    @Test
    fun `pausing queued upload does not cancel or advance current upload`() {
        val current = uploadAttachment(messageTid = 54L, state = TransferState.Uploading)
        val queued = uploadAttachment(messageTid = 55L, state = TransferState.WaitingToUpload)
        val queuedTask = transferTask(queued)

        uploadFile(current, transferTask(current))
        uploadFile(queued, queuedTask)
        pauseUpload(queued, TransferState.WaitingToUpload)

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(transport.uploadCalls.single().cancelled).isFalse()

        transport.uploadCalls.single().succeed("current-url")
        assertThat(transport.uploadCalls).hasSize(1)

        resumeUpload(queued, TransferState.PauseUpload)
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:55")
    }

    @Test
    fun `upload failure is forwarded and advances queue`() {
        val first = uploadAttachment(messageTid = 56L)
        val second = uploadAttachment(messageTid = 57L)
        val firstTask = transferTask(first)
        var result: SceytResponse<String>? = null
        firstTask.uploadResultCallback = TransferResultCallback { result = it }

        uploadFile(first, firstTask)
        uploadFile(second, transferTask(second))
        transport.uploadCalls.single().fail(IllegalStateException("upload failed"))

        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("upload failed")
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:57")
    }

    @Test
    fun `transport cancellation is forwarded as failure and advances queue`() {
        val first = uploadAttachment(messageTid = 56L)
        val second = uploadAttachment(messageTid = 57L)
        val results = mutableListOf<SceytResponse<String>>()
        val firstTask = transferTask(first).apply {
            uploadResultCallback = TransferResultCallback { results += it }
        }

        uploadFile(first, firstTask)
        uploadFile(second, transferTask(second))
        transport.uploadCalls.single().fail(CancellationException("transport cancelled"))

        assertThat(results).hasSize(1)
        assertThat(results.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(results.single().message).isEqualTo("transport cancelled")
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:57")
    }

    @Test
    fun `transport timeout fails upload and next queued upload can complete`() {
        SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
            override suspend fun upload(request: FileUploadRequest, callback: FileTransferCallback) =
                withTimeout(10.milliseconds) { transport.upload(request, callback) }
        }
        val first = uploadAttachment(messageTid = 56L)
        val second = uploadAttachment(messageTid = 57L)
        val firstResults = mutableListOf<SceytResponse<String>>()
        val secondResults = mutableListOf<SceytResponse<String>>()
        val firstTask = transferTask(first).apply {
            uploadResultCallback = TransferResultCallback { firstResults += it }
        }
        val secondTask = transferTask(second).apply {
            uploadResultCallback = TransferResultCallback { secondResults += it }
        }

        uploadFile(first, firstTask)
        uploadFile(second, secondTask)
        testScope.advanceTimeBy(10.milliseconds)
        testScope.runCurrent()

        assertThat(firstResults).hasSize(1)
        assertThat(firstResults.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(transport.uploadCalls.first().cancelled).isTrue()
        assertThat(transport.uploadCalls).hasSize(2)
        transport.uploadCalls.last().succeed("second-url")
        assertThat(secondResults.single().data).isEqualTo("second-url")
    }

    @Test
    fun `shared transport cancellation fails every active member and allows a new share`() {
        val first = uploadAttachment(messageTid = 56L)
        val second = uploadAttachment(messageTid = 57L, filePath = first.filePath)
        val firstResults = mutableListOf<SceytResponse<String>>()
        val secondResults = mutableListOf<SceytResponse<String>>()
        val firstTask = transferTask(first).apply {
            uploadResultCallback = TransferResultCallback { firstResults += it }
        }
        val secondTask = transferTask(second).apply {
            uploadResultCallback = TransferResultCallback { secondResults += it }
        }

        uploadSharedFile(first, firstTask)
        uploadSharedFile(second, secondTask)
        transport.uploadCalls.single().fail(CancellationException("transport cancelled"))

        assertThat(firstResults).hasSize(1)
        assertThat(secondResults).hasSize(1)
        assertThat(firstResults.single().message).isEqualTo("transport cancelled")
        assertThat(secondResults.single().message).isEqualTo("transport cancelled")
        val next = uploadAttachment(messageTid = 58L, filePath = first.filePath)
        uploadSharedFile(next, transferTask(next))
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `interruption reports its cause and advances queue`() {
        val first = uploadAttachment(messageTid = 92L)
        val second = uploadAttachment(messageTid = 93L)
        val firstTask = transferTask(first)
        var result: SceytResponse<String>? = null
        firstTask.uploadResultCallback = TransferResultCallback { result = it }

        uploadFile(first, firstTask)
        uploadFile(second, transferTask(second))
        val firstCall = transport.uploadCalls.single()
        firstCall.interrupt()

        assertThat(firstCall.cancelled).isFalse()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("Network interrupted")
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:93")
    }

    @Test
    fun `throwing result callback is invoked once and does not block queue`() {
        val first = uploadAttachment(messageTid = 89L)
        val second = uploadAttachment(messageTid = 90L)
        var callbackCount = 0
        val firstTask = transferTask(first).apply {
            uploadResultCallback = TransferResultCallback {
                callbackCount++
                error("callback failed")
            }
        }

        uploadFile(first, firstTask)
        uploadFile(second, transferTask(second))
        transport.uploadCalls.single().succeed("first-url")

        assertThat(callbackCount).isEqualTo(1)
        assertThat(transport.uploadCalls.map { it.request.operationId })
            .containsExactly("upload:89", "upload:90").inOrder()
    }

    @Test
    fun `empty upload result is forwarded as failure and advances queue`() {
        val first = uploadAttachment(messageTid = 62L)
        val second = uploadAttachment(messageTid = 63L)
        val firstTask = transferTask(first)
        var result: SceytResponse<String>? = null
        firstTask.uploadResultCallback = TransferResultCallback { result = it }

        uploadFile(first, firstTask)
        uploadFile(second, transferTask(second))
        transport.uploadCalls.single().succeed(" ")

        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("File upload returned an empty remote reference")
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:63")
    }

    @Test
    fun `missing upload source is forwarded as failure and advances queue`() {
        val first = attachment(
            messageTid = 64L,
            filePath = null,
            originalFilePath = null,
        )
        val second = uploadAttachment(messageTid = 65L)
        val firstTask = transferTask(first)
        var result: SceytResponse<String>? = null
        firstTask.uploadResultCallback = TransferResultCallback { result = it }

        uploadFile(first, firstTask)
        uploadFile(second, transferTask(second))

        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("Attachment source path is missing")
        assertThat(transport.uploadCalls.single().request.operationId).isEqualTo("upload:65")
    }

    @Test
    fun `nonexistent upload source is forwarded as failure`() {
        val sourceFile = File(context.cacheDir, "upload-coordinator-tests/missing.txt")
        sourceFile.delete()
        val attachment = attachment(
            messageTid = 66L,
            filePath = sourceFile.path,
            originalFilePath = sourceFile.path,
        )
        val task = transferTask(attachment)
        var result: SceytResponse<String>? = null
        task.uploadResultCallback = TransferResultCallback { result = it }

        uploadFile(attachment, task)

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo(
            "Attachment source file does not exist: ${sourceFile.path}",
        )
    }

    @Test
    fun `preparation failure is forwarded and advances queue`() {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        val first = uploadAttachment(messageTid = 60L)
        val second = uploadAttachment(messageTid = 61L)
        val firstTask = transferTask(first)
        val checksumStarted = CompletableDeferred<Unit>()
        val releaseChecksum = CompletableDeferred<Unit>()
        var result: SceytResponse<String>? = null
        firstTask.uploadResultCallback = TransferResultCallback { result = it }
        doSuspendableAnswer { invocation ->
            if (invocation.getArgument<String?>(0) == first.originalFilePath) {
                checksumStarted.complete(Unit)
                releaseChecksum.await()
                throw IllegalStateException("checksum failed")
            }
            null
        }.whenever(attachmentLogic) { getFileChecksumData(org.mockito.kotlin.any()) }

        uploadFile(first, firstTask)
        uploadFile(second, transferTask(second))
        releaseChecksum.complete(Unit)

        assertThat(checksumStarted.isCompleted).isTrue()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("checksum failed")
        assertThat(transport.uploadCalls.single().request.operationId).isEqualTo("upload:61")
    }

    @Test
    fun `preparation cancellation with active job is forwarded and advances queue`() {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        val first = uploadAttachment(messageTid = 60L)
        val second = uploadAttachment(messageTid = 61L)
        val results = mutableListOf<SceytResponse<String>>()
        val firstTask = transferTask(first).apply {
            uploadResultCallback = TransferResultCallback { results += it }
        }
        val releaseChecksum = CompletableDeferred<Unit>()
        doSuspendableAnswer { invocation ->
            if (invocation.getArgument<String?>(0) == first.originalFilePath) {
                releaseChecksum.await()
                throw CancellationException("checksum cancelled")
            }
            null
        }.whenever(attachmentLogic) { getFileChecksumData(org.mockito.kotlin.any()) }

        uploadFile(first, firstTask)
        uploadFile(second, transferTask(second))
        releaseChecksum.complete(Unit)

        assertThat(results).hasSize(1)
        assertThat(results.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(results.single().message).isEqualTo("checksum cancelled")
        assertThat(transport.uploadCalls.single().request.operationId).isEqualTo("upload:61")
    }

    @Test
    fun `preparing upload pauses without an active transport operation`() {
        val attachment = uploadAttachment(messageTid = 52L, state = TransferState.Preparing)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }

        pauseUpload(attachment, TransferState.Preparing)

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(task.state).isEqualTo(TransferState.PauseUpload)
        assertThat(states).containsExactly(TransferState.PauseUpload)
    }

    @Test
    fun `pause cancels upload while checksum is being prepared`() {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        val attachment = uploadAttachment(
            messageTid = 59L,
            state = TransferState.Preparing,
        )
        val task = transferTask(attachment)
        val checksumStarted = CompletableDeferred<Unit>()
        val releaseChecksum = CompletableDeferred<Unit>()
        doSuspendableAnswer {
            checksumStarted.complete(Unit)
            releaseChecksum.await()
            null
        }.whenever(attachmentLogic) { getFileChecksumData(attachment.originalFilePath) }

        uploadFile(attachment, task)
        assertThat(checksumStarted.isCompleted).isTrue()

        pauseUpload(attachment, TransferState.Preparing)
        releaseChecksum.complete(Unit)

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(task.state).isEqualTo(TransferState.PauseUpload)
    }

    @Test
    fun `late transcode result after pause is ignored and resume can transcode again`() {
        transport.pauseResult = true
        val transcodeCallbacks = mutableListOf<(Result<String>) -> Unit>()
        @Suppress("UNCHECKED_CAST")
        val extensionsClass = Class.forName(
            "com.sceyt.chatuikit.persistence.extensions.AttachmentResizeExtensionsKt",
        ) as Class<Any>

        Mockito.mockStatic(extensionsClass) { invocation ->
            if (invocation.method.name == "transcodeVideo") {
                @Suppress("UNCHECKED_CAST")
                transcodeCallbacks += invocation.arguments[4] as (Result<String>) -> Unit
                null
            } else {
                invocation.callRealMethod()
            }
        }.use {
            val attachment = uploadAttachment(
                messageTid = 67L,
                type = AttachmentTypeEnum.Video.value,
                state = TransferState.Preparing,
            )
            val task = transferTask(attachment)

            uploadFile(attachment, task)
            assertThat(transcodeCallbacks).hasSize(1)

            pauseUpload(attachment, TransferState.Preparing)
            transcodeCallbacks.single().invoke(Result.success("late-transcoded.mp4"))

            assertThat(transport.pauseCalls).isEmpty()
            assertThat(transport.uploadCalls).isEmpty()
            assertThat(task.state).isEqualTo(TransferState.PauseUpload)

            resumeUpload(attachment, TransferState.PauseUpload)

            assertThat(transcodeCallbacks).hasSize(2)
            assertThat(transport.uploadCalls).isEmpty()
        }
    }

    @Test
    fun `quick video resume completes and advances the upload queue`() {
        checkNotNull(SceytKoinApp.koinApp).koin.loadModules(
            listOf(module { single<Application> { context as Application } }),
        )
        val destinations = mutableListOf<String>()
        var activeListener: CompressionListener? = null
        Mockito.mockStatic(CustomVideoCompressor::class.java) { invocation ->
            when (invocation.method.name) {
                "start" -> {
                    destinations += invocation.getArgument<String>(3)
                    activeListener = invocation.getArgument<CompressionListener>(5)
                    Job()
                }

                "cancel" -> null
                else -> invocation.callRealMethod()
            }
        }.use {
            try {
                val video = uploadAttachment(
                    messageTid = 100L,
                    type = AttachmentTypeEnum.Video.value,
                    state = TransferState.Preparing,
                )
                val next = uploadAttachment(messageTid = 101L)
                val results = mutableListOf<String?>()
                val task = transferTask(video).apply {
                    uploadResultCallback = TransferResultCallback { results += it.data }
                }

                uploadFile(video, task)
                pauseUpload(video, TransferState.Preparing)
                resumeUpload(video, TransferState.PauseUpload)
                uploadFile(next, transferTask(next))

                assertThat(destinations).hasSize(1)
                checkNotNull(activeListener).also { activeListener = null }.onCancelled()

                assertThat(destinations).hasSize(2)
                assertThat(transport.uploadCalls).isEmpty()
                File(destinations.last()).writeText("transcoded video")
                checkNotNull(activeListener).also { activeListener = null }.onSuccess()
                transport.uploadCalls.single().succeed("video-url")

                assertThat(results).containsExactly("video-url")
                assertThat(transport.uploadCalls.map { it.request.operationId })
                    .containsExactly("upload:100", "upload:101").inOrder()
                transport.uploadCalls.last().succeed("next-url")
            } finally {
                coordinator.cancelAll()
                activeListener?.onCancelled()
            }
        }
    }

    @Test
    fun `pausing shared transcode preserves regular upload of same file`() {
        verifyPausingOneTranscodeKeepsOtherUpload(pauseShared = true)
    }

    @Test
    fun `pausing regular transcode preserves shared upload of same file`() {
        verifyPausingOneTranscodeKeepsOtherUpload(pauseShared = false)
    }

    @Test
    fun `shared preparation stops only after every task is paused`() {
        val transcodeCallbacks = mutableListOf<(Result<String>) -> Unit>()
        @Suppress("UNCHECKED_CAST")
        val extensionsClass = Class.forName(
            "com.sceyt.chatuikit.persistence.extensions.AttachmentResizeExtensionsKt",
        ) as Class<Any>

        Mockito.mockStatic(extensionsClass) { invocation ->
            if (invocation.method.name == "transcodeVideo") {
                @Suppress("UNCHECKED_CAST")
                transcodeCallbacks += invocation.arguments[4] as (Result<String>) -> Unit
                null
            } else {
                invocation.callRealMethod()
            }
        }.use {
            val first = uploadAttachment(
                messageTid = 94L,
                type = AttachmentTypeEnum.Video.value,
                state = TransferState.Preparing,
            )
            val second = uploadAttachment(
                messageTid = 95L,
                type = AttachmentTypeEnum.Video.value,
                state = TransferState.Preparing,
                filePath = first.filePath,
            )

            uploadSharedFile(first, transferTask(first))
            uploadSharedFile(second, transferTask(second))
            pauseUpload(first, TransferState.Preparing)

            assertThat(transcodeCallbacks).hasSize(1)

            pauseUpload(second, TransferState.Preparing)
            transcodeCallbacks.single().invoke(Result.success("late-transcoded.mp4"))

            assertThat(transport.pauseCalls).isEmpty()
            assertThat(transport.uploadCalls).isEmpty()

            resumeUpload(first, TransferState.PauseUpload)

            assertThat(transcodeCallbacks).hasSize(2)
        }
    }

    @Test
    fun `resume during image resize starts a replacement upload`() {
        val resizeStarted = CountDownLatch(1)
        val releaseResize = CountDownLatch(1)
        val resizeCalls = AtomicInteger()
        @Suppress("UNCHECKED_CAST")
        val extensionsClass = Class.forName(
            "com.sceyt.chatuikit.persistence.extensions.AttachmentResizeExtensionsKt",
        ) as Class<Any>
        val attachment = uploadAttachment(
            messageTid = 91L,
            type = AttachmentTypeEnum.Image.value,
            state = TransferState.Preparing,
        )
        val task = transferTask(attachment)

        Mockito.mockStatic(extensionsClass) { invocation ->
            if (invocation.method.name == "resizeImage") {
                if (resizeCalls.incrementAndGet() == 1) {
                    resizeStarted.countDown()
                    releaseResize.await(2, TimeUnit.SECONDS)
                }
                invocation.arguments[0] as String
            } else {
                invocation.callRealMethod()
            }
        }.use {
            val pauseCompleted = CountDownLatch(1)
            val pauser = thread {
                resizeStarted.await(2, TimeUnit.SECONDS)
                pauseUpload(attachment, TransferState.Preparing)
                pauseCompleted.countDown()
                releaseResize.countDown()
            }

            uploadFile(attachment, task)
            assertThat(pauseCompleted.await(2, TimeUnit.SECONDS)).isTrue()
            resumeUpload(attachment, TransferState.PauseUpload)
            testScope.runCurrent()
            pauser.join(2_000)

            assertThat(resizeCalls.get()).isEqualTo(2)
            assertThat(transport.uploadCalls).hasSize(1)
        }
    }

    @Test
    fun `late result from cancelled upload does not complete replacement`() {
        val attachment = uploadAttachment(messageTid = 53L, state = TransferState.Uploading)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        val results = mutableListOf<String?>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }
        task.uploadResultCallback = TransferResultCallback { results += it.data }

        uploadFile(attachment, task)
        val pausedCall = transport.uploadCalls.single()
        pauseUpload(attachment, TransferState.Uploading)
        resumeUpload(attachment, TransferState.PauseUpload)
        val replacementCall = transport.uploadCalls.last()

        pausedCall.progress(30f)
        pausedCall.succeed("late-url")

        assertThat(results).isEmpty()
        assertThat(states).containsExactly(
            TransferState.PauseUpload,
            TransferState.WaitingToUpload,
        ).inOrder()

        pauseUpload(attachment, TransferState.Uploading)

        assertThat(replacementCall.cancelled).isTrue()
        assertThat(states).containsExactly(
            TransferState.PauseUpload,
            TransferState.WaitingToUpload,
            TransferState.PauseUpload,
        ).inOrder()
        assertThat(results).isEmpty()
    }

    @Test
    fun `shared preparation failure is forwarded to every shared task`() {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        val first = uploadAttachment(messageTid = 70L)
        val second = uploadAttachment(messageTid = 71L, filePath = first.filePath)
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        val firstResults = mutableListOf<SceytResponse<String>>()
        val secondResults = mutableListOf<SceytResponse<String>>()
        firstTask.uploadResultCallback = TransferResultCallback { firstResults += it }
        secondTask.uploadResultCallback = TransferResultCallback { secondResults += it }
        val releaseChecksum = CompletableDeferred<Unit>()
        doSuspendableAnswer { invocation ->
            if (invocation.getArgument<String?>(0) == first.originalFilePath) {
                releaseChecksum.await()
                throw IllegalStateException("shared checksum failed")
            }
            null
        }.whenever(attachmentLogic) { getFileChecksumData(org.mockito.kotlin.any()) }

        uploadSharedFile(first, firstTask)
        uploadSharedFile(second, secondTask)
        releaseChecksum.complete(Unit)

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(firstResults).hasSize(1)
        assertThat(secondResults).hasSize(1)
        assertThat(firstResults.single().message).isEqualTo("shared checksum failed")
        assertThat(secondResults.single().message).isEqualTo("shared checksum failed")
    }

    @Test
    fun `cancel all stops active and queued uploads and allows a new upload`() {
        val active = uploadAttachment(messageTid = 80L)
        val queued = uploadAttachment(messageTid = 81L)
        val next = uploadAttachment(messageTid = 82L)
        val results = mutableListOf<SceytResponse<String>>()
        val activeTask = transferTask(active).apply {
            uploadResultCallback = TransferResultCallback { results += it }
        }

        uploadFile(active, activeTask)
        uploadFile(queued, transferTask(queued))
        coordinator.cancelAll()
        uploadFile(next, transferTask(next))

        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls[0].cancelled).isTrue()
        assertThat(transport.uploadCalls[1].request.operationId).isEqualTo("upload:82")
        assertThat(results).isEmpty()
    }

    private fun verifyPausingOneTranscodeKeepsOtherUpload(pauseShared: Boolean) {
        checkNotNull(SceytKoinApp.koinApp).koin.loadModules(
            listOf(module { single<Application> { context as Application } }),
        )
        val destinations = mutableListOf<String>()
        var activeListener: CompressionListener? = null
        Mockito.mockStatic(CustomVideoCompressor::class.java) { invocation ->
            when (invocation.method.name) {
                "start" -> {
                    destinations += invocation.getArgument<String>(3)
                    activeListener = invocation.getArgument<CompressionListener>(5)
                    Job()
                }

                "cancel" -> null
                else -> invocation.callRealMethod()
            }
        }.use {
            try {
                val shared = uploadAttachment(
                    messageTid = 100L,
                    type = AttachmentTypeEnum.Video.value,
                    state = TransferState.Preparing,
                )
                val regular = uploadAttachment(
                    messageTid = 101L,
                    type = AttachmentTypeEnum.Video.value,
                    state = TransferState.Preparing,
                    filePath = shared.filePath,
                )
                val results = mutableListOf<String?>()
                val remaining = if (pauseShared) regular else shared
                val sharedTask = transferTask(shared)
                val regularTask = transferTask(regular)
                val remainingTask = if (pauseShared) regularTask else sharedTask
                remainingTask.uploadResultCallback = TransferResultCallback { results += it.data }

                if (pauseShared) {
                    uploadSharedFile(shared, sharedTask)
                    uploadFile(regular, regularTask)
                } else {
                    uploadFile(regular, regularTask)
                    uploadSharedFile(shared, sharedTask)
                }
                pauseUpload(if (pauseShared) shared else regular, TransferState.Preparing)
                checkNotNull(activeListener).also { activeListener = null }.onCancelled()

                assertThat(destinations).hasSize(2)
                File(destinations.last()).writeText("transcoded video")
                checkNotNull(activeListener).also { activeListener = null }.onSuccess()

                assertThat(transport.uploadCalls.single().request.operationId)
                    .isEqualTo("upload:${remaining.messageTid}")
                transport.uploadCalls.single().succeed("video-url")
                assertThat(results).containsExactly("video-url")

                val next = uploadAttachment(messageTid = 102L)
                uploadFile(next, transferTask(next))
                assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:102")
                transport.uploadCalls.last().succeed("next-url")
            } finally {
                coordinator.cancelAll()
                activeListener?.onCancelled()
            }
        }
    }

    @Test
    fun `pause requested from a progress callback prevents the queued upload`() {
        val first = uploadAttachment(messageTid = 1L)
        val second = uploadAttachment(messageTid = 2L)
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        var triggered = false
        firstTask.progressCallback = ProgressUpdateCallback {
            if (triggered) return@ProgressUpdateCallback
            triggered = true
            uploadFile(second, secondTask)
            pauseUpload(second, TransferState.WaitingToUpload)
        }
        uploadFile(first, firstTask)

        transport.uploadCalls.single().progress(50f)
        transport.uploadCalls.single().succeed("first-url")

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(secondTask.state).isEqualTo(TransferState.PauseUpload)
    }

    @Test
    fun `cancel requested from a progress callback removes the queued upload`() {
        val first = uploadAttachment(messageTid = 1L)
        val second = uploadAttachment(messageTid = 2L)
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        var triggered = false
        firstTask.progressCallback = ProgressUpdateCallback {
            if (triggered) return@ProgressUpdateCallback
            triggered = true
            uploadFile(second, secondTask)
            service.cancelUpload(second)
        }
        uploadFile(first, firstTask)

        transport.uploadCalls.single().progress(50f)
        transport.uploadCalls.single().succeed("first-url")

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(service.findTransferTask(second)).isNull()
    }

    @Test
    fun `stale waiting pause after completed upload is ignored`() {
        val attachment = uploadAttachment(messageTid = 181L, state = TransferState.WaitingToUpload)
        AttachmentTransferStateStore.put(
            TransferData(attachment.messageTid, 100f, TransferState.Uploaded, attachment.filePath, "done-url"),
        )

        pauseUpload(attachment, TransferState.WaitingToUpload)

        assertThat(service.findTransferTask(attachment)).isNull()
    }

    private fun uploadFile(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        assertThat(service.upload(attachment)).isSameInstanceAs(task)
    }

    private fun uploadSharedFile(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        assertThat(service.uploadSharedFile(attachment)).isSameInstanceAs(task)
    }

    private fun cancelUpload(attachment: SceytAttachment) {
        service.cancelUpload(attachment)
    }

    private fun pauseUpload(attachment: SceytAttachment, state: TransferState) {
        service.pause(attachment.messageTid, attachment, state)
    }

    private fun resumeUpload(attachment: SceytAttachment, state: TransferState) {
        check(state in listOf(TransferState.PendingUpload, TransferState.PauseUpload, TransferState.ErrorUpload))
        val task = service.findOrCreateTransferTask(attachment)
        task.state = TransferState.WaitingToUpload
        if (task.isSharedUpload) service.uploadSharedFile(attachment) else service.upload(attachment)
    }

    private fun transferTask(attachment: SceytAttachment): TransferTask =
        service.findOrCreateTransferTask(attachment).apply {
            progressCallback = null
            preparingCallback = null
            resumePauseCallback = null
            uploadResultCallback = null
            updateFileLocationCallback = null
        }

    private fun uploadAttachment(
        messageTid: Long = 10L,
        name: String = "attachment.txt",
        type: String = AttachmentTypeEnum.File.value,
        state: TransferState? = TransferState.PendingUpload,
        filePath: String? = null,
    ): SceytAttachment {
        val sourceFile = filePath?.let(::File)
            ?: File(context.cacheDir, "upload-coordinator-tests/$messageTid-$name")
        sourceFile.parentFile?.mkdirs()
        sourceFile.createNewFile()

        return attachment(
            messageTid = messageTid,
            name = name,
            type = type,
            filePath = sourceFile.path,
            originalFilePath = sourceFile.path,
            state = state,
        )
    }
}
