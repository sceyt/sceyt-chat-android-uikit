package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.logger.SceytLoggerImpl
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.presentation.components.channel_list.channels.data.ChannelEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

/**
 * Covers the [ChannelEvent]s the channel actions dialog can produce, so the global search
 * Chats tab applies the same actions as the channel list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelActionsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val channelInteractor = mock<ChannelInteractor>()
    private val systemMessageSender = mock<SystemMessageSender>()

    private lateinit var viewModel: ChannelActionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // The default logger delegates to android.util.Log, which is not mocked in JVM tests.
        SceytLog.setLogger(SceytLogLevel.None) { _, _, _, _ -> }

        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<ChannelInteractor> { channelInteractor }
                single<SystemMessageSender> { systemMessageSender }
            })
        }

        runBlocking {
            whenever(channelInteractor.markChannelAsRead(any()))
                .thenReturn(SceytResponse.Success<SceytChannel>(null))
            whenever(channelInteractor.markChannelAsUnRead(any()))
                .thenReturn(SceytResponse.Success<SceytChannel>(null))
            whenever(channelInteractor.muteChannel(any(), any()))
                .thenReturn(SceytResponse.Success<SceytChannel>(null))
            whenever(channelInteractor.unMuteChannel(any()))
                .thenReturn(SceytResponse.Success<SceytChannel>(null))
            whenever(channelInteractor.pinChannel(any()))
                .thenReturn(SceytResponse.Success<SceytChannel>(null))
            whenever(channelInteractor.unpinChannel(any()))
                .thenReturn(SceytResponse.Success<SceytChannel>(null))
            whenever(channelInteractor.leaveChannel(any()))
                .thenReturn(SceytResponse.Success<Long>(null))
            whenever(channelInteractor.deleteChannel(any()))
                .thenReturn(SceytResponse.Success<Long>(null))
        }

        viewModel = ChannelActionsViewModel(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        SceytLog.setLogger(SceytLogLevel.Verbose, SceytLoggerImpl())
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `pin event pins the channel`() = runTest(dispatcher) {
        viewModel.onChannelCommandEvent(ChannelEvent.Pin(fakeChannel(CHANNEL_ID)))
        advanceUntilIdle()

        verifyBlocking(channelInteractor) { pinChannel(CHANNEL_ID) }
    }

    @Test
    fun `unpin event unpins the channel`() = runTest(dispatcher) {
        viewModel.onChannelCommandEvent(ChannelEvent.UnPin(fakeChannel(CHANNEL_ID)))
        advanceUntilIdle()

        verifyBlocking(channelInteractor) { unpinChannel(CHANNEL_ID) }
    }

    @Test
    fun `mark as read event marks the channel as read`() = runTest(dispatcher) {
        viewModel.onChannelCommandEvent(ChannelEvent.MarkAsRead(fakeChannel(CHANNEL_ID)))
        advanceUntilIdle()

        verifyBlocking(channelInteractor) { markChannelAsRead(CHANNEL_ID) }
    }

    @Test
    fun `mark as unread event marks the channel as unread`() = runTest(dispatcher) {
        viewModel.onChannelCommandEvent(ChannelEvent.MarkAsUnRead(fakeChannel(CHANNEL_ID)))
        advanceUntilIdle()

        verifyBlocking(channelInteractor) { markChannelAsUnRead(CHANNEL_ID) }
    }

    @Test
    fun `mute event forwards the chosen mute until`() = runTest(dispatcher) {
        val muteUntil = 1_700_000_000_000L

        viewModel.onChannelCommandEvent(ChannelEvent.Mute(fakeChannel(CHANNEL_ID), muteUntil))
        advanceUntilIdle()

        verifyBlocking(channelInteractor) { muteChannel(CHANNEL_ID, muteUntil) }
    }

    @Test
    fun `unmute event unmutes the channel`() = runTest(dispatcher) {
        viewModel.onChannelCommandEvent(ChannelEvent.UnMute(fakeChannel(CHANNEL_ID)))
        advanceUntilIdle()

        verifyBlocking(channelInteractor) { unMuteChannel(CHANNEL_ID) }
    }

    @Test
    fun `delete event deletes the channel`() = runTest(dispatcher) {
        viewModel.onChannelCommandEvent(ChannelEvent.DeleteChannel(fakeChannel(CHANNEL_ID)))
        advanceUntilIdle()

        verifyBlocking(channelInteractor) { deleteChannel(CHANNEL_ID) }
    }

    @Test
    fun `leave event on a group sends the member left message and leaves`() = runTest(dispatcher) {
        viewModel.onChannelCommandEvent(
            ChannelEvent.LeaveChannel(fakeChannel(CHANNEL_ID, isGroup = true))
        )
        advanceUntilIdle()

        verifyBlocking(systemMessageSender) { sendMemberLeft(CHANNEL_ID) }
        verifyBlocking(channelInteractor) { leaveChannel(CHANNEL_ID) }
    }

    @Test
    fun `leave event on a direct channel does not send a system message`() = runTest(dispatcher) {
        viewModel.onChannelCommandEvent(
            ChannelEvent.LeaveChannel(fakeChannel(CHANNEL_ID, isGroup = false))
        )
        advanceUntilIdle()

        verifyBlocking(systemMessageSender, never()) { sendMemberLeft(any()) }
        verifyBlocking(channelInteractor) { leaveChannel(CHANNEL_ID) }
    }

    @Test
    fun `leave still leaves the channel when the system message fails`() = runTest(dispatcher) {
        runBlocking {
            whenever(systemMessageSender.sendMemberLeft(any()))
                .thenThrow(RuntimeException("send failed"))
        }

        viewModel.onChannelCommandEvent(
            ChannelEvent.LeaveChannel(fakeChannel(CHANNEL_ID, isGroup = true))
        )
        advanceUntilIdle()

        verifyBlocking(channelInteractor) { leaveChannel(CHANNEL_ID) }
    }

    private fun fakeChannel(id: Long, isGroup: Boolean = false): SceytChannel {
        val channel = mock<SceytChannel>()
        whenever(channel.id).thenReturn(id)
        whenever(channel.isGroup).thenReturn(isGroup)
        return channel
    }

    private companion object {
        const val CHANNEL_ID = 42L
    }
}
