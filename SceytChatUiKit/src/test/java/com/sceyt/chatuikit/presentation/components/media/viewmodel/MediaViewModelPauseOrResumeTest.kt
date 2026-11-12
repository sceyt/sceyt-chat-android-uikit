package com.sceyt.chatuikit.presentation.components.media.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewTransferHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewModelPauseOrResumeTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val fileTransferService = mock<FileTransferService>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        SceytLog.setLogger(SceytLogLevel.Verbose) { _, _, _, _ -> }
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<AttachmentInteractor> { mock() }
                single<MessageInteractor> { mock() }
                single<FileTransferService> { fileTransferService }
                factory { PauseOrResumeTransferUseCase(mock(), fileTransferService) }
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
    fun `tap on a failed download starts it again`() = runTest(dispatcher) {
        val viewModel = viewModel(TransferState.ErrorDownload)
        val item = viewModel.mediaItems.value.single()

        viewModel.pauseOrResumeTransfer(item)
        advanceUntilIdle()

        verify(fileTransferService, timeout(TIMEOUT_MS)).download(eq(item.attachment), any())
    }

    @Test
    fun `tap on a paused download resumes it`() = runTest(dispatcher) {
        val viewModel = viewModel(TransferState.PauseDownload)
        val item = viewModel.mediaItems.value.single()

        viewModel.pauseOrResumeTransfer(item)
        advanceUntilIdle()

        verify(fileTransferService, timeout(TIMEOUT_MS))
            .resume(eq(MESSAGE_TID), eq(item.attachment), eq(TransferState.PauseDownload), eq(CHANNEL_ID))
    }

    @Test
    fun `tap on a running download pauses it`() = runTest(dispatcher) {
        val viewModel = viewModel(TransferState.Downloading)
        val item = viewModel.mediaItems.value.single()

        viewModel.pauseOrResumeTransfer(item)
        advanceUntilIdle()

        verify(fileTransferService, timeout(TIMEOUT_MS))
            .pause(eq(MESSAGE_TID), eq(item.attachment), eq(TransferState.Downloading))
    }

    private fun viewModel(state: TransferState) = MediaViewModel(
        reversed = false,
        channelId = CHANNEL_ID,
        mediaTypes = listOf(AttachmentTypeEnum.Image.value),
        preloadedData = MediaPreviewTransferHolder.PreloadedData(
            items = listOf(AttachmentWithUserData(attachment(state), user = null)),
            initialIndex = 0,
        ),
        ioDispatcher = dispatcher,
    )

    private fun attachment(state: TransferState) = SceytAttachment(
        id = 1L,
        messageId = 1L,
        messageTid = MESSAGE_TID,
        userId = null,
        name = "photo.jpg",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 0L,
        createdAt = 1000L,
        url = "https://cdn.example/1",
        filePath = null,
        transferState = state,
        progressPercent = 0f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private companion object {
        const val MESSAGE_TID = 11L
        const val CHANNEL_ID = 100L
        const val TIMEOUT_MS = 1_000L
    }
}
