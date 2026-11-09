package com.sceyt.chatuikit.persistence.logicimpl.message

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentPayLoadEntity
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.repositories.MessagesRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PersistenceMessagesLogicImplPreviewLinkTest {
    private val messageDao = mock<MessageDao>()
    private val repository = mock<MessagesRepository>()
    private val channels = mock<PersistenceChannelsLogic>()

    @Before
    fun setUp() {
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module { single { channels } })
        }
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `reconnect send of an uploaded video links its saved preview`() = runBlocking {
        whenever(messageDao.getPendingMessages(CHANNEL_ID)).thenReturn(listOf(uploadedVideoMessageWithPreview()))
        whenever(repository.sendMessage(any(), any())).thenReturn(SceytResponse.Error())

        logic().sendPendingMessages(CHANNEL_ID)

        val sent = argumentCaptor<SceytMessage>()
        verify(repository).sendMessage(eq(CHANNEL_ID), sent.capture())
        val video = sent.firstValue.attachments.orEmpty().single { it.type == AttachmentTypeEnum.Video.value }
        assertThat(JSONObject(checkNotNull(video.metadata)).getString(SceytConstants.Preview))
            .isEqualTo(PREVIEW_URL)
    }

    private fun uploadedVideoMessageWithPreview() = run {
        val video = attachment(
            messageTid = TID,
            name = "video.mp4",
            type = AttachmentTypeEnum.Video.value,
            url = VIDEO_URL,
            state = TransferState.Uploaded,
        )
        val preview = attachment(
            messageTid = TID,
            name = "video_preview.jpg",
            type = AttachmentTypeEnum.Preview.value,
            url = PREVIEW_URL,
            state = null,
        )
        val messageDb = createMessage(createdAt = 1L, tid = TID)
            .copy(channelId = CHANNEL_ID, deliveryStatus = MessageDeliveryStatus.Pending, attachments = listOf(video, preview))
            .toMessageDb(unList = false)
        val payload = AttachmentPayLoadEntity(
            messageTid = TID,
            transferState = TransferState.Uploaded,
            progressPercent = 100f,
            url = VIDEO_URL,
            filePath = video.filePath,
        )
        messageDb.copy(attachments = messageDb.attachments?.map {
            if (it.attachmentEntity.type == AttachmentTypeEnum.Video.value) it.copy(payLoad = payload) else it
        })
    }

    private fun logic() = PersistenceMessagesLogicImpl(
        context = RuntimeEnvironment.getApplication(),
        messageDao = messageDao,
        rangeDao = mock(),
        attachmentDao = mock(),
        pendingMarkerDao = mock(),
        reactionDao = mock(),
        userDao = mock(),
        pendingMessageStateDao = mock(),
        pendingMessageDeleteByTidDao = mock(),
        pollDao = mock(),
        pinnedMessageDao = mock(),
        pendingPinDao = mock(),
        pendingPollVoteDao = mock(),
        fileTransferService = mock(),
        messagesRepository = repository,
        preference = mock(),
        messagesCache = mock(),
        channelCache = mock(),
        messageLoadRangeUpdater = mock(),
        checkDeletedMessagesUseCase = mock(),
        channelSyncStateStore = mock(),
    )

    private companion object {
        const val CHANNEL_ID = 7L
        const val TID = 42L
        const val VIDEO_URL = "https://cdn.test/video.mp4"
        const val PREVIEW_URL = "https://cdn.test/video_preview.jpg"
    }
}
