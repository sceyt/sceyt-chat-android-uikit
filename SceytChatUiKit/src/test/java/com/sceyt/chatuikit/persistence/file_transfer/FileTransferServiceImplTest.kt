package com.sceyt.chatuikit.persistence.file_transfer

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.impl.WorkManagerImpl
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logicimpl.AttachmentDownloadCoordinator
import com.sceyt.chatuikit.persistence.logicimpl.AttachmentUploadCoordinator
import com.sceyt.chatuikit.persistence.logicimpl.RecordingFileTransferTransport
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.RESUME_PAUSED_UPLOAD
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FileTransferServiceImplTest {
    @get:Rule
    val files = TemporaryFolder()
    private val workManager = mock<WorkManagerImpl>()
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var transport: RecordingFileTransferTransport
    private var uploadCoordinator: AttachmentUploadCoordinator? = null

    companion object {
        // FileTransferHelper retains injected dependencies for the lifetime of its singleton.
        private val uploader = mock<AttachmentUploadCoordinator>()
        private val downloads by lazy {
            AttachmentDownloadCoordinator(RuntimeEnvironment.getApplication(), TestScope(UnconfinedTestDispatcher()))
        }
        private val service by lazy {
            FileTransferServiceImpl(RuntimeEnvironment.getApplication(), uploader, downloads, mock())
        }
    }

    @Before
    fun setUp() {
        previousFileTransfer = SceytChatUIKit.fileTransfer
        service.cancelAllTransfers()
        reset(uploader)
        WorkManagerImpl.setDelegate(workManager)
        val continuation = mock<WorkContinuation>()
        whenever(workManager.beginUniqueWork(any(), any(), any<OneTimeWorkRequest>()))
            .thenReturn(continuation)
        whenever(continuation.enqueue()).thenReturn(mock<Operation>())
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<FileTransferService> { service }
                single<PersistenceAttachmentLogic> { mock() }
                single<FileChecksumDao> { mock() }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) {
                    Dispatchers.Unconfined
                }
            })
        }
        transport = RecordingFileTransferTransport()
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@FileTransferServiceImplTest.transport
            destinationProvider = FileTransferDestinationProvider { _, attachment ->
                File(files.root, "${attachment.id}.txt")
            }
        }
    }

    @After
    fun tearDown() {
        uploadCoordinator?.cancelAll()
        service.cancelAllTransfers()
        SceytChatUIKit.fileTransfer = previousFileTransfer
        WorkManagerImpl.setDelegate(null)
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `task is configured before upload starts`() {
        val attachment = attachment()
        var configuredTask: TransferTask? = null
        doAnswer {
            assertThat(configuredTask).isSameInstanceAs(it.arguments[1])
        }.`when`(uploader).uploadFile(eq(attachment), any(), any(), any())

        val task = service.upload(attachment) {
            configuredTask = this
        }

        assertThat(task).isSameInstanceAs(configuredTask)
        verify(uploader).uploadFile(eq(attachment), eq(task), any(), any())
    }

    @Test
    fun `duplicate download reuses its active task`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val updatedAttachment = attachment.copy(progressPercent = 40f)

        val firstTask = service.download(attachment)
        val duplicateTask = service.download(updatedAttachment)

        assertThat(duplicateTask).isSameInstanceAs(firstTask)
        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(transport.downloadCalls.single().request.url).isEqualTo(attachment.url)
    }

    @Test
    fun `duplicate recovery download keeps its task and delivers completion`() {
        val attachment = attachment(state = TransferState.Downloaded)
        service.redownload(attachment)
        val task = checkNotNull(service.findTransferTask(attachment))
        var result: Result<SceytAttachment>? = null
        task.addOnCompletionListener("recovery") { result = it }

        service.redownload(attachment)

        assertThat(service.findTransferTask(attachment)).isSameInstanceAs(task)
        val call = transport.downloadCalls.single()
        call.succeed(call.request.destinationFile.path)
        assertThat(result?.getOrNull()?.filePath).isEqualTo(call.request.destinationFile.path)
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `paused download resumes on first user tap without a registered task`() = runBlocking {
        val attachment = attachment(state = TransferState.PauseDownload)
        val useCase = PauseOrResumeTransferUseCase(RuntimeEnvironment.getApplication(), service)
        assertThat(service.findTransferTask(attachment)).isNull()

        useCase(attachment)

        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(transport.downloadCalls.single().request.url).isEqualTo(attachment.url)
        assertThat(service.findTransferTask(attachment)?.state).isEqualTo(TransferState.Downloading)
    }

    @Test
    fun `user tap resumes an existing native download without starting another`() = runBlocking {
        transport.pauseResult = true
        transport.resumeResult = true
        val attachment = attachment(state = TransferState.PendingDownload)
        val useCase = PauseOrResumeTransferUseCase(RuntimeEnvironment.getApplication(), service)
        val task = service.download(attachment)
        service.pause(attachment.messageTid, attachment, TransferState.Downloading)

        useCase(attachment.copy(transferState = TransferState.PauseDownload))

        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(transport.resumeCalls).containsExactly(transport.downloadCalls.single().request.operationId)
        assertThat(task.state).isEqualTo(TransferState.Downloading)
    }

    @Test
    fun `download task keeps its identity when url changes`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val updatedAttachment = attachment.copy(url = "updated-url")

        val task = service.download(attachment)
        val reusedTask = service.download(updatedAttachment)

        assertThat(reusedTask).isSameInstanceAs(task)
        assertThat(service.findTransferTask(updatedAttachment)).isSameInstanceAs(task)
        assertThat(transport.downloadCalls).hasSize(1)
    }

    @Test
    fun `different attachment ids keep tasks separate even with the same url`() {
        val first = attachment(state = TransferState.PendingDownload)
        val second = first.copy(id = 11L, name = "second.txt")

        val firstTask = service.download(first)
        val secondTask = service.download(second)

        assertThat(secondTask).isNotSameInstanceAs(firstTask)
        assertThat(service.findTransferTask(first)).isSameInstanceAs(firstTask)
        assertThat(service.findTransferTask(second)).isSameInstanceAs(secondTask)
    }

    @Test
    fun `attachments without assigned ids use url to separate tasks`() {
        for (id in listOf(null, 0L)) {
            val first = attachment(state = TransferState.PendingDownload).copy(id = id)
            val second = first.copy(name = "second.txt", url = "second-url")

            val firstTask = service.download(first)
            val secondTask = service.download(second)

            assertThat(first.transferKey).isEqualTo("${first.messageTid}:url:${first.url}")
            assertThat(secondTask).isNotSameInstanceAs(firstTask)
            assertThat(service.download(first.copy(progressPercent = 40f)))
                .isSameInstanceAs(firstTask)
        }
    }

    @Test
    fun `downloads from one message use separate tasks and supplied attachments`() {
        val first = attachment(state = TransferState.PendingDownload)
        val second = first.copy(id = 11L, name = "second.txt", url = "second-url")

        val firstTask = service.download(first)
        val secondTask = service.download(second)

        assertThat(secondTask).isNotSameInstanceAs(firstTask)
        assertThat(service.findTransferTask(first)).isSameInstanceAs(firstTask)
        assertThat(service.findTransferTask(second)).isSameInstanceAs(secondTask)
        assertThat(transport.downloadCalls.map { it.request.url })
            .containsExactly(first.url, second.url).inOrder()
    }

    @Test
    fun `completing one download leaves the other attachment task registered`() {
        useTransferCoordinators()
        val first = attachment(state = TransferState.PendingDownload)
        val second = first.copy(id = 11L, name = "second.txt", url = "second-url")
        service.download(first)
        val secondTask = service.download(second)

        transport.downloadCalls.first().succeed(transport.downloadCalls.first().request.destinationFile.path)

        assertThat(service.findTransferTask(first)).isNull()
        assertThat(service.findTransferTask(second)).isSameInstanceAs(secondTask)

        transport.downloadCalls.last().succeed(transport.downloadCalls.last().request.destinationFile.path)

        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `failed download leaves the other attachment task registered`() {
        useTransferCoordinators()
        val first = attachment(state = TransferState.PendingDownload)
        val second = first.copy(id = 11L, name = "second.txt", url = "second-url")
        service.download(first)
        val secondTask = service.download(second)

        transport.downloadCalls.first().fail(SceytException(7, "download failed"))

        assertThat(service.findTransferTask(first)).isNull()
        assertThat(service.findTransferTask(second)).isSameInstanceAs(secondTask)
    }

    @Test
    fun `download notifies completion listener`() {
        useTransferCoordinators()
        val attachment = attachment(state = TransferState.PendingDownload)
        var result: Result<SceytAttachment>? = null

        service.download(attachment) {
            addOnCompletionListener("test") { result = it }
        }
        val call = transport.downloadCalls.single()
        call.succeed(call.request.destinationFile.path)

        assertThat(result?.getOrNull()?.filePath).isEqualTo(call.request.destinationFile.path)
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `retry from completion listener retains the new task and completes once`() {
        useTransferCoordinators()
        val attachment = attachment(state = TransferState.PendingDownload)
        var firstCompletions = 0
        var retryCompletions = 0
        var retryTask: TransferTask? = null
        service.download(attachment) {
            addOnCompletionListener("first") { result ->
                firstCompletions++
                assertThat(result.isFailure).isTrue()
                retryTask = service.download(attachment) {
                    addOnCompletionListener("retry") { retryCompletions++ }
                }
            }
        }

        transport.downloadCalls.first().fail(SceytException(7, "download failed"))

        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(service.findTransferTask(attachment)).isSameInstanceAs(retryTask)
        transport.downloadCalls.last().succeed(transport.downloadCalls.last().request.destinationFile.path)
        assertThat(firstCompletions).isEqualTo(1)
        assertThat(retryCompletions).isEqualTo(1)
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `upload creates a new task after terminal cleanup`() {
        val attachment = attachment()
        val completedTask = service.upload(attachment)

        service.removeTransferTask(completedTask)
        val retryTask = service.upload(attachment)

        assertThat(retryTask).isNotSameInstanceAs(completedTask)
    }

    @Test
    fun `late cleanup does not remove a replacement task`() {
        val attachment = attachment()
        val oldTask = service.upload(attachment)
        service.removeTransferTask(oldTask)
        val replacement = service.upload(attachment)

        service.removeTransferTask(oldTask)

        assertThat(service.findTransferTask(attachment)).isSameInstanceAs(replacement)
    }

    @Test
    fun `upload result removes a task even when its url changes`() {
        useTransferCoordinators()
        val attachment = attachment(url = null, filePath = files.newFile("source.txt").path).copy(id = null)
        val task = service.upload(attachment)

        transport.uploadCalls.single().succeed("uploaded-url")

        assertThat(task.attachment.url).isEqualTo("uploaded-url")
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `custom upload result callback does not change the url returned to the worker`() {
        useTransferCoordinators()
        val attachment = attachment(url = null, filePath = files.newFile("custom-result.txt").path)
        var completedAttachment: SceytAttachment? = null
        var callbackCount = 0
        service.upload(attachment) {
            uploadResultCallback = TransferResultCallback { callbackCount++ }
            addOnCompletionListener("worker") { completedAttachment = it.getOrThrow() }
        }

        transport.uploadCalls.single().succeed("uploaded-url")

        assertThat(callbackCount).isEqualTo(1)
        assertThat(completedAttachment?.url).isEqualTo("uploaded-url")
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `throwing upload callback still completes listeners and releases the queue`() {
        useTransferCoordinators()
        val first = attachment(messageTid = 25L, filePath = files.newFile("callback-error.txt").path)
        val next = attachment(messageTid = 26L, filePath = files.newFile("next.txt").path)
        var completions = 0
        service.upload(first) {
            uploadResultCallback = TransferResultCallback {
                assertThat(service.findTransferTask(first)).isNull()
                throw IllegalStateException("customer callback failed")
            }
            addOnCompletionListener("worker") { result ->
                assertThat(result.isSuccess).isTrue()
                assertThat(result.getOrThrow().url).isEqualTo("first-url")
                completions++
            }
        }
        service.upload(next)

        transport.uploadCalls.first().succeed("first-url")

        assertThat(completions).isEqualTo(1)
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:26")
        assertThat(service.findTransferTask(first)).isNull()
    }

    @Test
    fun `upload retry inside result callback keeps its own task and listener`() {
        useTransferCoordinators()
        val attachment = attachment(messageTid = 27L, filePath = files.newFile("retry.txt").path)
        var firstCompletions = 0
        var retryCompletions = 0
        var retryTask: TransferTask? = null
        service.upload(attachment) {
            uploadResultCallback = TransferResultCallback {
                assertThat(service.findTransferTask(attachment)).isNull()
                retryTask = service.upload(attachment) {
                    addOnCompletionListener("retry") { retryCompletions++ }
                }
            }
            addOnCompletionListener("first") { firstCompletions++ }
        }

        transport.uploadCalls.first().fail(SceytException(7, "upload failed"))

        assertThat(service.findTransferTask(attachment)).isSameInstanceAs(retryTask)
        assertThat(firstCompletions).isEqualTo(1)
        assertThat(retryCompletions).isEqualTo(0)
        transport.uploadCalls.last().succeed("retry-url")
        assertThat(retryCompletions).isEqualTo(1)
        assertThat(service.getTasks()).isEmpty()
    }

    private fun useTransferCoordinators() {
        val context = RuntimeEnvironment.getApplication()
        val scope = TestScope(UnconfinedTestDispatcher())
        transport = RecordingFileTransferTransport()
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@FileTransferServiceImplTest.transport
            destinationProvider = FileTransferDestinationProvider { _, attachment ->
                File(files.root, "${attachment.id}.txt")
            }
        }
        val uploads = AttachmentUploadCoordinator(context, mock(), scope)
        uploadCoordinator = uploads
        doAnswer {
            uploads.uploadFile(it.getArgument(0), it.getArgument(1), it.getArgument(2), it.getArgument(3))
            Unit
        }.`when`(uploader).uploadFile(any(), any(), any(), any())
        doAnswer { uploads.cancel(it.getArgument(0)) }.`when`(uploader).cancel(any())
        doAnswer { uploads.cancelAll() }.`when`(uploader).cancelAll()
    }

    @Test
    fun `cancel upload removes its task before cancellation and releases listeners`() {
        val attachment = attachment()
        var completion: Result<SceytAttachment>? = null
        val task = service.upload(attachment) {
            addOnCompletionListener("worker") { completion = it }
        }
        val originalState = task.state
        doAnswer {
            assertThat(service.findTransferTask(attachment)).isNull()
        }.`when`(uploader).cancel(task)

        service.cancelUpload(attachment)

        verify(uploader).cancel(task)
        verify(uploader, never()).pause(any(), any())
        assertThat(completion?.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(task.onCompletionListeners).isEmpty()
        assertThat(task.state).isEqualTo(originalState)
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `cancel upload without a task does not create one`() {
        val attachment = attachment()

        service.cancelUpload(attachment)

        verify(uploader, never()).uploadFile(any(), any(), any(), any())
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `upload cancelled during configuration never starts and releases its listener`() {
        useTransferCoordinators()
        val attachment = attachment(url = null, filePath = files.newFile("cancel-before-admission.txt").path)
        var completion: Result<SceytAttachment>? = null

        service.upload(attachment) {
            service.cancelUpload(attachment)
            addOnCompletionListener("worker") { completion = it }
        }

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(completion?.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `cancellation from resume callback prevents queued upload admission`() {
        useTransferCoordinators()
        val attachment = attachment(url = null, filePath = files.newFile("cancel-on-resume.txt").path)
        val task = service.findOrCreateTransferTask(attachment)
        task.resumePauseCallback = ResumePauseCallback { service.cancelUpload(attachment) }

        service.resume(attachment.messageTid, attachment, TransferState.PendingUpload)

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `cancel upload leaves other attachment tasks registered`() {
        val first = attachment()
        val second = first.copy(id = 11L, url = "second-url")
        service.upload(first)
        val secondTask = service.upload(second)

        service.cancelUpload(first)

        assertThat(service.findTransferTask(first)).isNull()
        assertThat(service.findTransferTask(second)).isSameInstanceAs(secondTask)
    }

    @Test
    fun `cancel upload stops its transport call and starts the next upload`() {
        useTransferCoordinators()
        val first = attachment(messageTid = 20L, url = null, filePath = files.newFile("first.txt").path)
        val second = attachment(messageTid = 21L, url = null, filePath = files.newFile("second.txt").path)
        var resultCalls = 0
        val firstTask = service.upload(first) {
            uploadResultCallback = TransferResultCallback { resultCalls++ }
        }
        service.upload(second)

        service.cancelUpload(first)

        assertThat(transport.uploadCalls.first().cancelled).isTrue()
        assertThat(service.findTransferTask(first)).isNull()
        assertThat(resultCalls).isEqualTo(0)
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:21")
    }

    @Test
    fun `cancel upload also stops a running download of the attachment`() {
        useTransferCoordinators()
        val attachment = attachment(messageTid = 23L, state = TransferState.PendingDownload)
        service.download(attachment)

        service.cancelUpload(attachment)

        assertThat(transport.downloadCalls.single().cancelled).isTrue()
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `cancel all transfers releases waiting completion listeners`() {
        val attachment = attachment(messageTid = 24L)
        var completion: Result<SceytAttachment>? = null
        val task = service.upload(attachment) {
            addOnCompletionListener("worker") { completion = it }
        }

        service.cancelAllTransfers()

        assertThat(completion?.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(task.onCompletionListeners).isEmpty()
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `cancel all during configuration prevents a late upload and releases its listener`() {
        useTransferCoordinators()
        val attachment = attachment(url = null, filePath = files.newFile("cancel-all-before-admission.txt").path)
        var completion: Result<SceytAttachment>? = null

        service.upload(attachment) {
            service.cancelAllTransfers()
            addOnCompletionListener("worker") { completion = it }
        }

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(completion?.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `cancel all clears tasks and coordinator state`() {
        val attachment = attachment()
        service.upload(attachment)

        service.cancelAllTransfers()

        assertThat(service.getTasks()).isEmpty()
        verify(uploader).cancelAll()
    }

    @Test
    fun `resume after succeeded worker schedules upload and send`() {
        assertResumeSchedulesWorker(WorkInfo.State.SUCCEEDED)
    }

    @Test
    fun `resume after failed worker schedules upload and send`() {
        assertResumeSchedulesWorker(WorkInfo.State.FAILED)
    }

    @Test
    fun `resume after cancelled worker schedules upload and send`() {
        assertResumeSchedulesWorker(WorkInfo.State.CANCELLED)
    }

    @Test
    fun `resume without previous worker schedules upload and send`() {
        assertResumeSchedulesWorker()
    }

    @Test
    fun `user resumed upload keeps the channel cancellation tag`() = runBlocking {
        val attachment = attachment(state = TransferState.PauseUpload)
        service.findOrCreateTransferTask(attachment)
        val useCase = PauseOrResumeTransferUseCase(RuntimeEnvironment.getApplication(), service)

        useCase(attachment, channelId = 456L)

        val request = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager).beginUniqueWork(
            eq(attachment.messageTid.toString()), eq(ExistingWorkPolicy.REPLACE), request.capture(),
        )
        assertThat(request.firstValue.tags).contains("456")
        assertThat(request.firstValue.tags).contains(attachment.messageTid.toString())
        assertThat(request.firstValue.workSpec.input.getBoolean(RESUME_PAUSED_UPLOAD, false)).isTrue()
    }

    @Test
    fun `user resume after restart schedules explicit resume with the channel tag`() = runBlocking {
        val attachment = attachment(state = TransferState.PauseUpload)
        assertThat(service.findTransferTask(attachment)).isNull()
        val useCase = PauseOrResumeTransferUseCase(RuntimeEnvironment.getApplication(), service)

        useCase(attachment, channelId = 456L)

        val request = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager).beginUniqueWork(
            eq(attachment.messageTid.toString()), eq(ExistingWorkPolicy.REPLACE), request.capture(),
        )
        assertThat(request.firstValue.tags).contains("456")
        assertThat(request.firstValue.tags).contains(attachment.messageTid.toString())
        assertThat(request.firstValue.workSpec.input.getBoolean(RESUME_PAUSED_UPLOAD, false)).isTrue()
    }

    @Test
    fun `error upload retry after finished worker schedules upload and send`() {
        assertResumeSchedulesWorker(WorkInfo.State.FAILED, transferState = TransferState.ErrorUpload)
    }

    @Test
    fun `resume replaces a paused worker even while it is finishing`() {
        assertResumeSchedulesWorker(WorkInfo.State.SUCCEEDED, WorkInfo.State.RUNNING)
    }

    private fun assertResumeSchedulesWorker(
        vararg workStates: WorkInfo.State,
        transferState: TransferState = TransferState.PauseUpload,
    ) {
        val attachment = attachment(state = transferState)
        setWorkStates(attachment.messageTid, *workStates)

        service.resume(attachment.messageTid, attachment, transferState)

        val request = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager).beginUniqueWork(
            eq(attachment.messageTid.toString()), eq(ExistingWorkPolicy.REPLACE), request.capture(),
        )
        assertThat(request.firstValue.workSpec.input.getBoolean(RESUME_PAUSED_UPLOAD, false)).isTrue()
        verify(uploader, never()).uploadFile(any(), any(), any(), any())
    }

    private fun setWorkStates(messageTid: Long, vararg states: WorkInfo.State) {
        val workInfos = states.map { state ->
            mock<WorkInfo>().also { whenever(it.state).thenReturn(state) }
        }
        whenever(workManager.getWorkInfosByTag(messageTid.toString()))
            .thenReturn(Futures.immediateFuture(workInfos))
    }
}
