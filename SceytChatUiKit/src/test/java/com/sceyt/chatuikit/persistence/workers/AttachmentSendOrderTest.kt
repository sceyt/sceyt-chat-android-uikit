package com.sceyt.chatuikit.persistence.workers

import androidx.work.WorkInfo
import androidx.work.impl.WorkManagerImpl
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentSendOrderTest {
    private val messageDao = mock<MessageDao>()
    private val workManager = mock<WorkManagerImpl>()
    private val uploadWorkStates = mutableMapOf<Long, MutableStateFlow<List<WorkInfo>>>()
    private val sendOrder = AttachmentSendOrder(RuntimeEnvironment.getApplication(), messageDao)

    @Before
    fun setUp() {
        WorkManagerImpl.setDelegate(workManager)
        whenever(workManager.getWorkInfosForUniqueWorkFlow(any())).thenAnswer {
            uploadWorkState(it.getArgument<String>(0).toLong())
        }
    }

    @After
    fun tearDown() {
        WorkManagerImpl.setDelegate(null)
    }

    @Test
    fun `message without previous pending messages continues at once`() = runTest {
        whenever(messageDao.getPreviousPendingMessageTids(MESSAGE_TID)).thenReturn(emptyList())

        val passed = awaitTurn()

        assertThat(passed()).isTrue()
    }

    @Test
    fun `previous running upload holds the message until it finishes`() = runTest {
        whenever(messageDao.getPreviousPendingMessageTids(MESSAGE_TID)).thenReturn(listOf(1L))
        uploadWorkState(1L).value = listOf(uploadWork(WorkInfo.State.RUNNING))

        val passed = awaitTurn()
        assertThat(passed()).isFalse()

        uploadWorkState(1L).value = listOf(uploadWork(WorkInfo.State.SUCCEEDED))
        runCurrent()
        assertThat(passed()).isTrue()
    }

    @Test
    fun `previous upload rescheduled by Android holds the message until it is cancelled`() = runTest {
        whenever(messageDao.getPreviousPendingMessageTids(MESSAGE_TID)).thenReturn(listOf(1L))
        uploadWorkState(1L).value = listOf(uploadWork(WorkInfo.State.ENQUEUED))

        val passed = awaitTurn()
        assertThat(passed()).isFalse()

        uploadWorkState(1L).value = listOf(uploadWork(WorkInfo.State.CANCELLED))
        runCurrent()
        assertThat(passed()).isTrue()
    }

    @Test
    fun `previous message without upload work does not hold the message`() = runTest {
        whenever(messageDao.getPreviousPendingMessageTids(MESSAGE_TID)).thenReturn(listOf(1L, 2L))
        uploadWorkState(2L).value = listOf(uploadWork(WorkInfo.State.FAILED))

        val passed = awaitTurn()

        assertThat(passed()).isTrue()
    }

    @Test
    fun `previous message that gets new upload work while waiting holds the message again`() = runTest {
        whenever(messageDao.getPreviousPendingMessageTids(MESSAGE_TID)).thenReturn(listOf(1L, 2L))
        uploadWorkState(2L).value = listOf(uploadWork(WorkInfo.State.RUNNING))

        val passed = awaitTurn()
        uploadWorkState(1L).value = listOf(uploadWork(WorkInfo.State.ENQUEUED))
        uploadWorkState(2L).value = listOf(uploadWork(WorkInfo.State.SUCCEEDED))
        runCurrent()
        assertThat(passed()).isFalse()

        uploadWorkState(1L).value = listOf(uploadWork(WorkInfo.State.SUCCEEDED))
        runCurrent()
        assertThat(passed()).isTrue()
    }

    private fun TestScope.awaitTurn(): () -> Boolean {
        var passed = false
        launch {
            sendOrder.awaitPreviousUploads(MESSAGE_TID)
            passed = true
        }
        runCurrent()
        return { passed }
    }

    private fun uploadWorkState(messageTid: Long): MutableStateFlow<List<WorkInfo>> =
        uploadWorkStates.getOrPut(messageTid) { MutableStateFlow(emptyList()) }

    private fun uploadWork(state: WorkInfo.State): WorkInfo =
        mock<WorkInfo>().also { whenever(it.state).thenReturn(state) }

    private companion object {
        const val MESSAGE_TID = 3L
    }
}
