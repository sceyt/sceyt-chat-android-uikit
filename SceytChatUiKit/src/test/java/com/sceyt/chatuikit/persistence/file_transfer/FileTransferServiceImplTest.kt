package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.impl.WorkManagerImpl
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.RESUME_PAUSED_UPLOAD
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
class FileTransferServiceImplTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val logic = mock<FileTransferLogic>()
    private val service = FileTransferServiceImpl(context, logic)
    private val workManager = mock<WorkManagerImpl>()

    @Before
    fun setUp() {
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
    }

    @After
    fun tearDown() {
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
        }.`when`(logic).uploadFile(eq(attachment), any())

        val task = service.upload(attachment) {
            configuredTask = this
        }

        assertThat(task).isSameInstanceAs(configuredTask)
        verify(logic).uploadFile(attachment, task)
    }

    @Test
    fun `duplicate download reuses its active task`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val updatedAttachment = attachment.copy(url = "updated-url")

        val firstTask = service.download(attachment)
        val duplicateTask = service.download(updatedAttachment)

        assertThat(duplicateTask).isSameInstanceAs(firstTask)
        verify(logic, times(2)).downloadFile(attachment, firstTask)
    }

    @Test
    fun `download notifies completion listener`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        var result: Result<SceytAttachment>? = null
        doAnswer {
            (it.arguments[1] as TransferTask).downloadCallback
                ?.onResult(SceytResponse.Success("downloaded-file"))
            Unit
        }.`when`(logic).downloadFile(eq(attachment), any())

        service.download(attachment) {
            addOnCompletionListener("test") { result = it }
        }

        assertThat(result?.getOrNull()?.filePath).isEqualTo("downloaded-file")
    }

    @Test
    fun `upload creates a new task after terminal cleanup`() {
        val attachment = attachment()
        val completedTask = service.upload(attachment)

        service.removeTransferTask(attachment.messageTid)
        val retryTask = service.upload(attachment)

        assertThat(retryTask).isNotSameInstanceAs(completedTask)
    }

    @Test
    fun `cancel all clears tasks and coordinator state`() {
        val attachment = attachment()
        service.upload(attachment)

        service.cancelAllTransfers()

        assertThat(service.getTasks()).isEmpty()
        verify(logic).cancelAll()
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
    fun `error upload retry after finished worker schedules upload and send`() {
        assertResumeSchedulesWorker(WorkInfo.State.FAILED, transferState = TransferState.ErrorUpload)
    }

    @Test
    fun `resume with a live worker keeps its completion listener`() {
        val attachment = attachment(state = TransferState.PauseUpload)
        setWorkStates(attachment.messageTid, WorkInfo.State.SUCCEEDED, WorkInfo.State.RUNNING)

        service.resume(attachment.messageTid, attachment, TransferState.PauseUpload)

        verify(logic).resumeLoad(attachment, TransferState.PauseUpload)
        verify(workManager, never()).beginUniqueWork(any(), any(), any<OneTimeWorkRequest>())
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
            eq(attachment.messageTid.toString()), eq(ExistingWorkPolicy.KEEP), request.capture(),
        )
        assertThat(request.firstValue.workSpec.input.getBoolean(RESUME_PAUSED_UPLOAD, false)).isTrue()
        verify(logic, never()).resumeLoad(any(), any())
    }

    private fun setWorkStates(messageTid: Long, vararg states: WorkInfo.State) {
        val workInfos = states.map { state ->
            mock<WorkInfo>().also { whenever(it.state).thenReturn(state) }
        }
        whenever(workManager.getWorkInfosByTag(messageTid.toString()))
            .thenReturn(Futures.immediateFuture(workInfos))
    }
}
