package com.sceyt.chatuikit.persistence.workers

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkContinuation
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentPayLoadEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.ForwardingDetailsDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PendingAttachmentRetryWorkerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val messageDao = mock<MessageDao>()
    private val workManager = mock<WorkManagerImpl>()

    @Before
    fun setUp() {
        WorkManagerImpl.setDelegate(workManager)
        val operation = mock<Operation>()
        whenever(operation.result).thenReturn(Futures.immediateFuture(Operation.SUCCESS))
        val continuation = mock<WorkContinuation>()
        whenever(continuation.enqueue()).thenReturn(operation)
        whenever(workManager.beginUniqueWork(any(), any(), any<OneTimeWorkRequest>())).thenReturn(continuation)
        whenever(workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())).thenReturn(operation)
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module { single<MessageDao> { messageDao } })
        }
    }

    @After
    fun tearDown() {
        WorkManagerImpl.setDelegate(null)
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `schedules an upload worker for each pending file message`() = runBlocking<Unit> {
        whenever(messageDao.getAllPendingMessages()).thenReturn(
            listOf(
                pendingFileMessage(tid = 1L, channelId = 7L),
                pendingFileMessage(tid = 2L, channelId = 8L, state = TransferState.Uploaded),
            )
        )

        val result = worker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(workManager).beginUniqueWork(eq("1"), eq(ExistingWorkPolicy.KEEP), any<OneTimeWorkRequest>())
        verify(workManager).beginUniqueWork(eq("2"), eq(ExistingWorkPolicy.KEEP), any<OneTimeWorkRequest>())
    }

    @Test
    fun `skips paused forwarded and text only messages`() = runBlocking<Unit> {
        val forwarded = pendingFileMessage(tid = 2L).let {
            val details = ForwardingDetailsDb(messageId = 1L, userId = "user", hops = 1)
            it.copy(messageEntity = it.messageEntity.copy(forwardingDetailsDb = details))
        }
        val textOnly = createMessage(createdAt = 1L, tid = 3L)
            .copy(deliveryStatus = MessageDeliveryStatus.Pending)
            .toMessageDb(unList = false)
        whenever(messageDao.getAllPendingMessages()).thenReturn(
            listOf(pendingFileMessage(tid = 1L, state = TransferState.PauseUpload), forwarded, textOnly)
        )

        val result = worker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(workManager, never()).beginUniqueWork(any(), any(), any<OneTimeWorkRequest>())
    }

    @Test
    fun `retry request waits for the network`() {
        PendingAttachmentRetryWorker.schedule(context)

        val request = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager).enqueueUniqueWork(
            eq("PendingAttachmentRetry"),
            eq(ExistingWorkPolicy.REPLACE),
            request.capture(),
        )
        assertThat(request.firstValue.workSpec.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
        assertThat(request.firstValue.tags).contains(SceytConstants.SCEYT_WORKER_TAG)
    }

    private fun pendingFileMessage(
        tid: Long,
        channelId: Long = 7L,
        state: TransferState = TransferState.ErrorUpload,
    ): MessageDb {
        val file = attachment(messageTid = tid, url = null, state = state)
        val messageDb = createMessage(createdAt = 1L, tid = tid)
            .copy(
                channelId = channelId,
                deliveryStatus = MessageDeliveryStatus.Pending,
                attachments = listOf(file),
            )
            .toMessageDb(unList = false)
        val payload = AttachmentPayLoadEntity(
            messageTid = tid,
            transferState = state,
            progressPercent = 0f,
            url = null,
            filePath = file.filePath,
        )
        return messageDb.copy(attachments = messageDb.attachments?.map { it.copy(payLoad = payload) })
    }

    private fun worker(): PendingAttachmentRetryWorker {
        val params = mock<WorkerParameters>()
        whenever(params.inputData).thenReturn(Data.EMPTY)
        return PendingAttachmentRetryWorker(context, params)
    }
}
