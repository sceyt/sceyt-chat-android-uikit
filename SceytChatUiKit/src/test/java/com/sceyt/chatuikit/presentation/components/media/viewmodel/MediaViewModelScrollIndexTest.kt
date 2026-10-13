package com.sceyt.chatuikit.presentation.components.media.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.SceytResponse
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

    private fun attachment(
        id: Long?,
        type: String = AttachmentTypeEnum.Image.value,
        messageTid: Long = id ?: 0L,
    ) = SceytAttachment(
        id = id,
        messageId = id ?: 0L,
        messageTid = messageTid,
        userId = null,
        name = "attachment-$id",
        type = type,
        metadata = null,
        fileSize = 0L,
        createdAt = (id ?: 0L) * 1000L,
        url = "https://cdn.example/$id",
        filePath = "/path/$id.jpg",
        transferState = null,
        progressPercent = 0f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private fun attachmentWithUserData(
        id: Long?,
        type: String = AttachmentTypeEnum.Image.value,
        messageTid: Long = id ?: 0L,
    ) = AttachmentWithUserData(
        attachment = attachment(id, type, messageTid),
        user = null,
    )

    private fun items(range: LongRange) = range.map { attachmentWithUserData(it) }

    private fun dbResponse(
        items: List<AttachmentWithUserData>,
        loadType: PaginationResponse.LoadType = LoadNear,
    ) = PaginationResponse.DBResponse(
        data = items,
        loadKey = LoadKeyData(),
        offset = 0,
        hasNext = false,
        hasPrev = false,
        loadType = loadType,
    )

    private fun preloadedViewModel(
        items: List<AttachmentWithUserData>,
        initialIndex: Int,
    ) = MediaViewModel(
        reversed = false,
        channelId = 100L,
        mediaTypes = listOf(AttachmentTypeEnum.Image.value),
        openedAttachmentData = null,
        preloadedData = MediaPreviewTransferHolder.PreloadedData(items, initialIndex),
    )

    private fun serverResponse(items: List<AttachmentWithUserData>) =
        PaginationResponse.ServerResponse(
            data = SceytResponse.Success(items),
            cacheData = items,
            loadKey = LoadKeyData(),
            offset = 0,
            hasDiff = true,
            hasNext = false,
            hasPrev = false,
            loadType = LoadNear,
            ignoredDb = false,
        )

    private suspend fun mockNear(responses: Flow<PaginationResponse<AttachmentWithUserData>>) {
        whenever(
            attachmentInteractor.getNearAttachments(
                conversationId = any(),
                attachmentId = any(),
                types = any(),
                offset = any(),
                ignoreDb = any(),
                loadKeyData = anyOrNull(),
            )
        ).thenReturn(responses)
    }

    private fun createViewModel(
        openedId: Long?,
        reversed: Boolean = false,
    ) = MediaViewModel(
        reversed = reversed,
        channelId = 100L,
        mediaTypes = listOf(AttachmentTypeEnum.Image.value),
        openedAttachmentData = attachmentWithUserData(openedId),
        preloadedData = null,
        ioDispatcher = dispatcher,
    )

    private fun TestScope.loadedViewModel(
        openedId: Long?,
        reversed: Boolean = false,
    ): MediaViewModel {
        val viewModel = createViewModel(openedId, reversed)
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `target is opened attachment and is found in initial list`() = runTest(dispatcher) {
        mockNear(emptyFlow())
        val viewModel = createViewModel(openedId = 10L)

        assertThat(viewModel.targetAttachmentId).isEqualTo(10L)
        assertThat(viewModel.findTargetIndex(viewModel.mediaItems.value)).isEqualTo(0)
    }

    @Test
    fun `target is found in db near list`() = runTest(dispatcher) {
        mockNear(flow { emit(dbResponse(items(8L..12L))) })
        val viewModel = loadedViewModel(openedId = 10L)

        val list = viewModel.mediaItems.value
        assertThat(list).hasSize(5)
        assertThat(list[viewModel.findTargetIndex(list)].attachment.id).isEqualTo(10L)
        assertThat(viewModel.findTargetIndex(list)).isEqualTo(2)
    }

    @Test
    fun `target is found in server list that adds older items after db list`() = runTest(dispatcher) {
        mockNear(flow {
            emit(dbResponse(items(8L..12L)))
            emit(serverResponse(items(5L..12L)))
        })
        val viewModel = loadedViewModel(openedId = 10L)

        val list = viewModel.mediaItems.value
        assertThat(list).hasSize(8)
        assertThat(viewModel.findTargetIndex(list)).isEqualTo(5)
    }

    @Test
    fun `target is found in reversed list`() = runTest(dispatcher) {
        mockNear(flow { emit(dbResponse(items(8L..12L))) })
        val viewModel = loadedViewModel(openedId = 9L, reversed = true)

        val list = viewModel.mediaItems.value
        assertThat(list.map { it.attachment.id }).containsExactly(12L, 11L, 10L, 9L, 8L).inOrder()
        assertThat(viewModel.findTargetIndex(list)).isEqualTo(3)
    }

    @Test
    fun `settled page becomes target for later lists`() = runTest(dispatcher) {
        val responses = Channel<PaginationResponse<AttachmentWithUserData>>(Channel.UNLIMITED)
        mockNear(responses.receiveAsFlow())
        val viewModel = createViewModel(openedId = 10L)

        responses.send(dbResponse(items(8L..12L)))
        advanceUntilIdle()
        viewModel.onPageSettled(viewModel.mediaItems.value.first { it.attachment.id == 12L })

        responses.send(serverResponse(items(5L..12L)))
        advanceUntilIdle()

        assertThat(viewModel.targetAttachmentId).isEqualTo(12L)
        val list = viewModel.mediaItems.value
        assertThat(list).hasSize(8)
        assertThat(viewModel.findTargetIndex(list)).isEqualTo(7)
        responses.close()
    }

    @Test
    fun `target index is -1 when list does not contain target`() = runTest(dispatcher) {
        mockNear(flow { emit(dbResponse(items(8L..12L))) })
        val viewModel = loadedViewModel(openedId = 10L)

        val withoutTarget = viewModel.mediaItems.value.filter { it.attachment.id != 10L }
        assertThat(viewModel.findTargetIndex(withoutTarget)).isEqualTo(-1)
    }

    @Test
    fun `opened attachment without id is found by reference after prev page is prepended`() =
        runTest(dispatcher) {
            whenever(
                attachmentInteractor.getPrevAttachments(
                    conversationId = any(),
                    lastAttachmentId = any(),
                    types = any(),
                    offset = any(),
                    ignoreDb = any(),
                    loadKeyData = anyOrNull(),
                )
            ).thenReturn(flow { emit(dbResponse(items(5L..7L), PaginationResponse.LoadType.LoadPrev)) })
            val viewModel = loadedViewModel(openedId = null)

            val list = viewModel.mediaItems.value
            assertThat(list.map { it.attachment.id }).containsExactly(5L, 6L, 7L, null).inOrder()
            assertThat(viewModel.targetAttachmentId).isNull()
            assertThat(viewModel.findTargetIndex(list)).isEqualTo(3)
        }

    @Test
    fun `target without id falls back to same message when placeholder is replaced`() =
        runTest(dispatcher) {
            val responses = Channel<PaginationResponse<AttachmentWithUserData>>(Channel.UNLIMITED)
            whenever(
                attachmentInteractor.getPrevAttachments(
                    conversationId = any(),
                    lastAttachmentId = any(),
                    types = any(),
                    offset = any(),
                    ignoreDb = any(),
                    loadKeyData = anyOrNull(),
                )
            ).thenReturn(responses.receiveAsFlow())
            val viewModel = MediaViewModel(
                reversed = false,
                channelId = 100L,
                mediaTypes = listOf(AttachmentTypeEnum.Image.value),
                openedAttachmentData = attachmentWithUserData(0L, messageTid = 2000L),
                preloadedData = null,
                ioDispatcher = dispatcher,
            )

            responses.send(
                serverResponse(
                    items(4L..7L) + attachmentWithUserData(20L, messageTid = 2000L) + attachmentWithUserData(30L),
                ).copy(loadType = PaginationResponse.LoadType.LoadPrev)
            )
            advanceUntilIdle()

            val list = viewModel.mediaItems.value
            assertThat(list.map { it.attachment.id }).containsExactly(4L, 5L, 6L, 7L, 20L, 30L).inOrder()
            assertThat(viewModel.findTargetIndex(list)).isEqualTo(4)
            responses.close()
        }

    @Test
    fun `target without id and message tid is not matched to other items`() =
        runTest(dispatcher) {
            val viewModel = preloadedViewModel(
                items = listOf(attachmentWithUserData(null), attachmentWithUserData(5L)),
                initialIndex = 0,
            )

            val otherList = listOf(attachmentWithUserData(null), attachmentWithUserData(5L))
            val stranger = preloadedViewModel(items = otherList, initialIndex = 0).mediaItems.value
            assertThat(viewModel.findTargetIndex(stranger)).isEqualTo(-1)
        }

    @Test
    fun `item without id is not matched by id against other items without id`() =
        runTest(dispatcher) {
            val viewModel = preloadedViewModel(
                items = listOf(attachmentWithUserData(null), attachmentWithUserData(null)),
                initialIndex = 1,
            )

            val list = viewModel.mediaItems.value
            assertThat(viewModel.findTargetIndex(list)).isEqualTo(1)
            viewModel.onPageSettled(list[0])
            assertThat(viewModel.findTargetIndex(list)).isEqualTo(0)
        }

    @Test
    fun `preloaded launch targets item at initial index`() = runTest(dispatcher) {
        val viewModel = preloadedViewModel(items = items(1L..3L), initialIndex = 2)
        advanceUntilIdle()

        assertThat(viewModel.targetAttachmentId).isEqualTo(3L)
        assertThat(viewModel.mediaItems.value).hasSize(3)
        assertThat(viewModel.findTargetIndex(viewModel.mediaItems.value)).isEqualTo(2)
    }

    @Test
    fun `preloaded launch targets item without id at initial index`() = runTest(dispatcher) {
        val viewModel = preloadedViewModel(
            items = listOf(attachmentWithUserData(1L), attachmentWithUserData(null), attachmentWithUserData(3L)),
            initialIndex = 1,
        )

        assertThat(viewModel.targetAttachmentId).isNull()
        assertThat(viewModel.findTargetIndex(viewModel.mediaItems.value)).isEqualTo(1)
    }

    @Test
    fun `preloaded launch keeps target when non media items are dropped`() = runTest(dispatcher) {
        val viewModel = preloadedViewModel(
            items = listOf(
                attachmentWithUserData(1L, AttachmentTypeEnum.File.value),
                attachmentWithUserData(2L),
                attachmentWithUserData(3L),
            ),
            initialIndex = 2,
        )

        val list = viewModel.mediaItems.value
        assertThat(list.map { it.attachment.id }).containsExactly(2L, 3L).inOrder()
        assertThat(viewModel.findTargetIndex(list)).isEqualTo(1)
    }

    @Test
    fun `preloaded launch has no target when initial item is not media`() = runTest(dispatcher) {
        val viewModel = preloadedViewModel(
            items = listOf(
                attachmentWithUserData(1L, AttachmentTypeEnum.File.value),
                attachmentWithUserData(2L),
            ),
            initialIndex = 0,
        )

        assertThat(viewModel.targetAttachmentId).isNull()
        assertThat(viewModel.findTargetIndex(viewModel.mediaItems.value)).isEqualTo(-1)
    }
}
