package com.sceyt.chatuikit.presentation.components.media.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewTransferHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Reproduces the bug where MediaViewModel.initialScrollIndex stays 0 after LoadNear
 * replaces the single-item list with a full page, causing the wrong attachment to be shown.
 *
 * Bug: opened attachment is at position N in loaded list, but initialScrollIndex = 0.
 * Fix: initialScrollIndex must be updated to the position of openedWithAttachment in the new list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewModelScrollIndexTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val attachmentInteractor = mock<AttachmentInteractor>()
    private val messageInteractor = mock<MessageInteractor>()
    private val fileTransferService = mock<FileTransferService>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        SceytLog.setLogger(SceytLogLevel.Verbose) { _, _, _, _ -> }
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<AttachmentInteractor> { attachmentInteractor }
                single<MessageInteractor> { messageInteractor }
                single<FileTransferService> { fileTransferService }
            })
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
        SceytKoinApp.koinApp = null
    }

    // region helpers

    private fun attachment(id: Long) = SceytAttachment(
        id = id,
        messageId = id,
        messageTid = id,
        userId = null,
        name = "attachment-$id",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 0L,
        createdAt = id * 1000L,
        url = "https://cdn.example/$id",
        filePath = "/path/$id.jpg",
        transferState = null,
        progressPercent = 0f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private fun attachmentWithUserData(id: Long) = AttachmentWithUserData(
        attachment = attachment(id),
        user = null,
    )

    private fun dbResponse(items: List<AttachmentWithUserData>) =
        PaginationResponse.DBResponse(
            data = items,
            loadKey = LoadKeyData(),
            offset = 0,
            hasNext = false,
            hasPrev = false,
            loadType = LoadNear,
        )

    // endregion

    /**
     * Scenario: user opens attachment with id=2.
     * DB returns [id=1, id=2, id=3] — opened attachment is at index 1.
     *
     * BUG (before fix):   initialScrollIndex = 0  → RecyclerView shows id=1 (wrong)
     * FIXED (after fix):  initialScrollIndex = 1  → RecyclerView shows id=2 (correct)
     */
    @Test
    fun `initialScrollIndex updated to correct position after LoadNear DB response`() =
        runTest(dispatcher) {
            val openedId = 2L
            val nearItems = listOf(
                attachmentWithUserData(1L),
                attachmentWithUserData(2L), // <-- opened attachment, position 1
                attachmentWithUserData(3L),
            )

            whenever(
                attachmentInteractor.getNearAttachments(
                    conversationId = any(),
                    attachmentId = any(),
                    types = any(),
                    offset = any(),
                    ignoreDb = any(),
                    loadKeyData = anyOrNull(),
                )
            ).thenReturn(flow { emit(dbResponse(nearItems)) })

            val viewModel = MediaViewModel(
                reversed = false,
                channelId = 100L,
                mediaTypes = listOf(AttachmentTypeEnum.Image.value),
                openedAttachmentData = attachmentWithUserData(openedId),
                preloadedData = null,
                ioDispatcher = dispatcher,
            )

            advanceUntilIdle()

            // After LoadNear, list has 3 items with opened attachment at index 1
            assertThat(viewModel.mediaItems.value).hasSize(3)
            assertThat(viewModel.mediaItems.value[1].attachment.id).isEqualTo(openedId)

            // BUG: initialScrollIndex is 0, but opened attachment is at index 1
            // After fix: this assertion should PASS (initialScrollIndex == 1)
            assertThat(viewModel.initialScrollIndex).isEqualTo(1)
        }

    /**
     * Scenario: opened attachment is first in list (position 0).
     * initialScrollIndex should remain 0 — no change needed.
     */
    @Test
    fun `initialScrollIndex stays 0 when opened attachment is first in list`() =
        runTest(dispatcher) {
            val openedId = 1L
            val nearItems = listOf(
                attachmentWithUserData(1L), // <-- opened attachment, position 0
                attachmentWithUserData(2L),
                attachmentWithUserData(3L),
            )

            whenever(
                attachmentInteractor.getNearAttachments(
                    conversationId = any(),
                    attachmentId = any(),
                    types = any(),
                    offset = any(),
                    ignoreDb = any(),
                    loadKeyData = anyOrNull(),
                )
            ).thenReturn(flow { emit(dbResponse(nearItems)) })

            val viewModel = MediaViewModel(
                reversed = false,
                channelId = 100L,
                mediaTypes = listOf(AttachmentTypeEnum.Image.value),
                openedAttachmentData = attachmentWithUserData(openedId),
                preloadedData = null,
                ioDispatcher = dispatcher,
            )

            advanceUntilIdle()

            assertThat(viewModel.mediaItems.value).hasSize(3)
            assertThat(viewModel.initialScrollIndex).isEqualTo(0)
        }

    /**
     * Scenario: preloaded launch — initialScrollIndex must stay as provided (not overwritten).
     */
    @Test
    fun `initialScrollIndex not changed for preloaded launch`() = runTest(dispatcher) {
        val preloadedItems = listOf(
            attachmentWithUserData(1L),
            attachmentWithUserData(2L),
            attachmentWithUserData(3L), // index 2
        )
        val preloadedData = MediaPreviewTransferHolder.PreloadedData(
            items = preloadedItems,
            initialIndex = 2,
        )

        val viewModel = MediaViewModel(
            reversed = false,
            channelId = 100L,
            mediaTypes = listOf(AttachmentTypeEnum.Image.value),
            openedAttachmentData = null,
            preloadedData = preloadedData,
        )

        advanceUntilIdle()

        // Preloaded: no DB load, initialScrollIndex stays as provided
        assertThat(viewModel.initialScrollIndex).isEqualTo(2)
        assertThat(viewModel.mediaItems.value).hasSize(3)
    }
}
