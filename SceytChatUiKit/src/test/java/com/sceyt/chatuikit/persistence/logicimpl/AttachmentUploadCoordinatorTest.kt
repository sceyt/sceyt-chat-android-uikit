package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ProgressUpdateCallback
import com.sceyt.chatuikit.persistence.file_transfer.ResumePauseCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferResultCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentUploadCoordinatorTest {
    private lateinit var context: Context
    private lateinit var service: TestFileTransferService
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
        service = TestFileTransferService()
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
        SceytKoinApp.koinApp = startKoin {
            modules(module { single<FileTransferService> { service } })
        }
        coordinator = AttachmentUploadCoordinator(context, attachmentLogic, testScope)
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previousFileTransfer
        SceytChatUIKit.config = previousConfig
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `regular uploads run sequentially`() {
        val first = uploadAttachment(messageTid = 1L)
        val second = uploadAttachment(messageTid = 2L)

        coordinator.uploadFile(first, transferTask(first))
        coordinator.uploadFile(second, transferTask(second))

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(transport.uploadCalls.single().request.operationId).isEqualTo("upload:1")

        transport.uploadCalls.first().succeed("first-url")

        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls[1].request.operationId).isEqualTo("upload:2")
    }

    @Test
    fun `duplicate queued upload is suppressed`() {
        val current = uploadAttachment(messageTid = 1L)
        val duplicate = uploadAttachment(messageTid = 2L)

        coordinator.uploadFile(current, transferTask(current))
        coordinator.uploadFile(duplicate, transferTask(duplicate))
        coordinator.uploadFile(duplicate, transferTask(duplicate))
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

        coordinator.uploadFile(attachment, task)
        val call = transport.uploadCalls.single()
        call.progress(55f)
        call.succeed("uploaded-url")

        assertThat(call.request.operationId).isEqualTo("upload:30")
        assertThat(call.request.sourceFile.path).isEqualTo(attachment.filePath)
        assertThat(call.request.fileName).isEqualTo("document.txt")
        assertThat(call.request.attachment).isEqualTo(attachment)
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

        coordinator.uploadFile(attachment, task)

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

        coordinator.uploadSharedFile(first, firstTask)
        coordinator.uploadSharedFile(second, secondTask)
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

        coordinator.uploadSharedFile(first, firstTask)
        coordinator.uploadSharedFile(second, secondTask)
        val call = transport.uploadCalls.single()

        coordinator.pauseLoad(first, TransferState.Uploading)
        call.progress(25f)

        assertThat(transport.pauseCalls).isEmpty()
        assertThat(firstProgress).isEmpty()
        assertThat(secondProgress).containsExactly(25f)

        coordinator.pauseLoad(second, TransferState.Uploading)

        assertThat(transport.pauseCalls).containsExactly("upload:3")
        assertThat(call.cancelled).isFalse()

        coordinator.resumeLoad(second, TransferState.PauseUpload)
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
        val coordinator = AttachmentUploadCoordinator(context, attachmentLogic, scope)
        val first = uploadAttachment(messageTid = 80L, state = TransferState.Uploading)
        val second = uploadAttachment(
            messageTid = 81L,
            filePath = first.filePath,
            state = TransferState.Uploading,
        )

        coordinator.uploadSharedFile(first, transferTask(first))
        coordinator.uploadSharedFile(second, transferTask(second))
        coordinator.pauseLoad(first, TransferState.Uploading)
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

        coordinator.uploadSharedFile(shared, transferTask(shared))
        coordinator.uploadSharedFile(follower, transferTask(follower))
        coordinator.uploadFile(regular, transferTask(regular).also(service::addTransferTask))
        coordinator.pauseLoad(regular, TransferState.Uploading)

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

        coordinator.uploadSharedFile(first, transferTask(first))
        coordinator.uploadSharedFile(second, transferTask(second))

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
            }
        }

        coordinator.uploadSharedFile(first, firstTask)
        coordinator.uploadSharedFile(second, secondTask)
        coordinator.pauseLoad(first, TransferState.Uploading)
        transport.uploadCalls.single().succeed("shared-url")

        coordinator.resumeLoad(first, TransferState.PauseUpload)

        assertThat(firstResults).containsExactly("shared-url")
        assertThat(transport.uploadCalls).hasSize(1)
    }

    @Test
    fun `native upload resume waits for its queue turn`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val first = uploadAttachment(messageTid = 5L, state = TransferState.Uploading)
        val second = uploadAttachment(messageTid = 6L)
        val firstTask = transferTask(first)
        service.addTransferTask(firstTask)

        coordinator.uploadFile(first, firstTask)
        coordinator.uploadFile(second, transferTask(second))
        val firstCall = transport.uploadCalls.single()

        coordinator.pauseLoad(first, TransferState.Uploading)

        assertThat(transport.pauseCalls).containsExactly("upload:5")
        assertThat(firstCall.cancelled).isFalse()
        assertThat(transport.uploadCalls.map { it.request.operationId })
            .containsExactly("upload:5", "upload:6").inOrder()

        coordinator.resumeLoad(first, TransferState.PauseUpload)

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
        service.addTransferTask(task)

        coordinator.uploadFile(attachment, task)
        coordinator.pauseLoad(attachment, TransferState.Uploading)
        coordinator.resumeLoad(attachment, TransferState.PauseUpload)

        assertThat(transport.resumeCalls).containsExactly("upload:7")
        assertThat(transport.uploadCalls.first().cancelled).isTrue()
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `pause cancels upload and resume requeues it`() {
        val attachment = uploadAttachment(messageTid = 40L, state = TransferState.Uploading)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }
        service.addTransferTask(task)

        coordinator.uploadFile(attachment, task)
        coordinator.pauseLoad(attachment, TransferState.Uploading)
        coordinator.resumeLoad(attachment, TransferState.PauseUpload)

        assertThat(transport.uploadCalls.first().cancelled).isTrue()
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(states).containsExactly(
            TransferState.PauseUpload,
            TransferState.WaitingToUpload,
        ).inOrder()
    }

    @Test
    fun `pausing active upload advances the sequential queue`() {
        val first = uploadAttachment(messageTid = 50L, state = TransferState.WaitingToUpload)
        val second = uploadAttachment(messageTid = 51L, state = TransferState.WaitingToUpload)
        val firstTask = transferTask(first)
        val states = mutableListOf<TransferState>()
        firstTask.resumePauseCallback = ResumePauseCallback { states += it.state }
        service.addTransferTask(firstTask)

        coordinator.uploadFile(first, firstTask)
        coordinator.uploadFile(second, transferTask(second))
        coordinator.pauseLoad(first, TransferState.WaitingToUpload)

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
        service.addTransferTask(task)

        coordinator.uploadFile(attachment, task)
        coordinator.pauseLoad(attachment, TransferState.Uploading)
        coordinator.uploadFile(attachment, task)

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(transport.uploadCalls.single().cancelled).isTrue()
    }

    @Test
    fun `pausing queued upload does not cancel or advance current upload`() {
        val current = uploadAttachment(messageTid = 54L, state = TransferState.Uploading)
        val queued = uploadAttachment(messageTid = 55L, state = TransferState.WaitingToUpload)
        val queuedTask = transferTask(queued)
        service.addTransferTask(queuedTask)

        coordinator.uploadFile(current, transferTask(current))
        coordinator.uploadFile(queued, queuedTask)
        coordinator.pauseLoad(queued, TransferState.WaitingToUpload)

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(transport.uploadCalls.single().cancelled).isFalse()

        transport.uploadCalls.single().succeed("current-url")
        assertThat(transport.uploadCalls).hasSize(1)

        coordinator.resumeLoad(queued, TransferState.PauseUpload)
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

        coordinator.uploadFile(first, firstTask)
        coordinator.uploadFile(second, transferTask(second))
        transport.uploadCalls.single().fail(IllegalStateException("upload failed"))

        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("upload failed")
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:57")
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

        coordinator.uploadFile(first, firstTask)
        coordinator.uploadFile(second, transferTask(second))
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

        coordinator.uploadFile(first, firstTask)
        coordinator.uploadFile(second, transferTask(second))
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

        coordinator.uploadFile(first, firstTask)
        coordinator.uploadFile(second, transferTask(second))

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

        coordinator.uploadFile(attachment, task)

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

        coordinator.uploadFile(first, firstTask)
        coordinator.uploadFile(second, transferTask(second))
        releaseChecksum.complete(Unit)

        assertThat(checksumStarted.isCompleted).isTrue()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("checksum failed")
        assertThat(transport.uploadCalls.single().request.operationId).isEqualTo("upload:61")
    }

    @Test
    fun `preparing upload pauses without an active transport operation`() {
        val attachment = uploadAttachment(messageTid = 52L, state = TransferState.Preparing)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }
        service.addTransferTask(task)

        coordinator.pauseLoad(attachment, TransferState.Preparing)

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
        service.addTransferTask(task)
        doSuspendableAnswer {
            checksumStarted.complete(Unit)
            releaseChecksum.await()
            null
        }.whenever(attachmentLogic) { getFileChecksumData(attachment.originalFilePath) }

        coordinator.uploadFile(attachment, task)
        assertThat(checksumStarted.isCompleted).isTrue()

        coordinator.pauseLoad(attachment, TransferState.Preparing)
        releaseChecksum.complete(Unit)

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(task.state).isEqualTo(TransferState.PauseUpload)
    }

    @Test
    fun `late transcode result after pause is ignored and resume can transcode again`() {
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
            service.addTransferTask(task)

            coordinator.uploadFile(attachment, task)
            assertThat(transcodeCallbacks).hasSize(1)

            coordinator.pauseLoad(attachment, TransferState.Preparing)
            transcodeCallbacks.single().invoke(Result.success("late-transcoded.mp4"))

            assertThat(transport.uploadCalls).isEmpty()
            assertThat(task.state).isEqualTo(TransferState.PauseUpload)

            coordinator.resumeLoad(attachment, TransferState.PauseUpload)

            assertThat(transcodeCallbacks).hasSize(2)
            assertThat(transport.uploadCalls).isEmpty()
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
        service.addTransferTask(task)

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
                coordinator.pauseLoad(attachment, TransferState.Preparing)
                pauseCompleted.countDown()
                releaseResize.countDown()
            }

            coordinator.uploadFile(attachment, task)
            assertThat(pauseCompleted.await(2, TimeUnit.SECONDS)).isTrue()
            coordinator.resumeLoad(attachment, TransferState.PauseUpload)
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
        service.addTransferTask(task)

        coordinator.uploadFile(attachment, task)
        val pausedCall = transport.uploadCalls.single()
        coordinator.pauseLoad(attachment, TransferState.Uploading)
        coordinator.resumeLoad(attachment, TransferState.PauseUpload)
        val replacementCall = transport.uploadCalls.last()

        pausedCall.progress(30f)
        pausedCall.succeed("late-url")

        assertThat(results).isEmpty()
        assertThat(states).containsExactly(
            TransferState.PauseUpload,
            TransferState.WaitingToUpload,
        ).inOrder()

        coordinator.pauseLoad(attachment, TransferState.Uploading)

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

        coordinator.uploadSharedFile(first, firstTask)
        coordinator.uploadSharedFile(second, secondTask)
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

        coordinator.uploadFile(active, transferTask(active))
        coordinator.uploadFile(queued, transferTask(queued))
        coordinator.cancelAll()
        coordinator.uploadFile(next, transferTask(next))

        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls[0].cancelled).isTrue()
        assertThat(transport.uploadCalls[1].request.operationId).isEqualTo("upload:82")
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
