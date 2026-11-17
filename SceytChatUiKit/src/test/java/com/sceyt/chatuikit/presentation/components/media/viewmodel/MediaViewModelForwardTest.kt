package com.sceyt.chatuikit.presentation.components.media.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewTransferHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewModelForwardTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val messageInteractor = mock<MessageInteractor>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<AttachmentInteractor> { mock() }
                single<MessageInteractor> { messageInteractor }
                single<FileTransferService> { mock() }
                single<PauseOrResumeTransferUseCase> { mock() }
            })
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
        SceytKoinApp.koinApp = null
    }

    @Test
    fun `message deleted for everyone is not forwardable`() = runTest(dispatcher) {
        whenever(messageInteractor.getMessageFromDbById(MESSAGE_ID))
            .thenReturn(message().copy(state = MessageState.Deleted))

        assertThat(viewModel().getForwardableMessage(MESSAGE_ID)).isNull()
    }

    @Test
    fun `hard deleted message is not forwardable`() = runTest(dispatcher) {
        whenever(messageInteractor.getMessageFromDbById(MESSAGE_ID))
            .thenReturn(message().copy(state = MessageState.DeletedHard))

        assertThat(viewModel().getForwardableMessage(MESSAGE_ID)).isNull()
    }

    @Test
    fun `message missing from db is not forwardable`() = runTest(dispatcher) {
        whenever(messageInteractor.getMessageFromDbById(MESSAGE_ID)).thenReturn(null)

        assertThat(viewModel().getForwardableMessage(MESSAGE_ID)).isNull()
    }

    @Test
    fun `existing message is forwardable`() = runTest(dispatcher) {
        val message = message()
        whenever(messageInteractor.getMessageFromDbById(MESSAGE_ID)).thenReturn(message)

        assertThat(viewModel().getForwardableMessage(MESSAGE_ID)).isEqualTo(message)
    }

    private fun message() = createMessage(createdAt = 1, id = MESSAGE_ID, tid = MESSAGE_ID)
        .copy(state = MessageState.Unmodified)

    private fun viewModel() = MediaViewModel(
        reversed = false,
        channelId = 1,
        mediaTypes = listOf(AttachmentTypeEnum.Image.value),
        preloadedData = MediaPreviewTransferHolder.PreloadedData(items = emptyList(), initialIndex = 0),
        ioDispatcher = dispatcher,
    )

    private companion object {
        const val MESSAGE_ID = 10L
    }
}
