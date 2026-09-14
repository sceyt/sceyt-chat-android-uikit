package com.sceyt.chatuikit.persistence.logicimpl

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.entity.messages.PinSyncStateEntity
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.persistence.logicimpl.usecases.ConfirmPinUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.PinMessageUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.RefreshPinnedMessageCacheUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendPendingPinsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.StorePinsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SyncChannelPinsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UnpinMessageUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UpdatePinnedMessagesUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.messageDb
import com.sceyt.chatuikit.persistence.logicimpl.usecases.messageEntity
import com.sceyt.chatuikit.persistence.logicimpl.usecases.pinnedEntity
import com.sceyt.chatuikit.persistence.logicimpl.usecases.sceytMessage
import com.sceyt.chatuikit.persistence.logicimpl.usecases.sceytPinnedMessage
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PinnedMessagesPersistenceTest {

    private lateinit var database: SceytDatabase
    private val pinRepository = mock<PinRepository>()
    private val refreshCache = mock<RefreshPinnedMessageCacheUseCase>()
    private val systemMessageSender = mock<SystemMessageSender>()
    private lateinit var logic: PersistencePinLogicImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(), SceytDatabase::class.java
        ).allowMainThreadQueries().build()
        val dao = database.pinnedMessageDao()
        val messages = database.messageDao()
        val confirm = ConfirmPinUseCase(dao)
        val send = SendPendingPinsUseCase(dao, pinRepository, confirm, systemMessageSender, refreshCache)
        val store = StorePinsUseCase(messages, dao, refreshCache)
        logic = PersistencePinLogicImpl(
            dao,
            PinMessageUseCase(messages, dao, send, refreshCache),
            UnpinMessageUseCase(dao, send, refreshCache),
            SyncChannelPinsUseCase(dao, pinRepository, send, store, refreshCache),
            send,
            UpdatePinnedMessagesUseCase(messages, dao, refreshCache, store),
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `failed unpin followed by successful sync preserves removal intent`() = runTest {
        val dao = database.pinnedMessageDao()
        database.messageDao().upsertMessage(messageDb())
        dao.upsertWithMirror(pinnedEntity(syncState = PinSyncStateEntity.Synced.value))
        whenever(pinRepository.unpinMessages(any(), any())).thenReturn(SceytResponse.Error(null))
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(
            flowOf(SceytPagingResponse.Success(listOf(sceytPinnedMessage()), hasNext = false))
        )

        logic.unpinMessage(7L, 42L)
        logic.syncChannelPins(7L)

        assertThat(dao.getByTid(42L, 7L)?.syncState).isEqualTo(PinSyncStateEntity.PendingUnpin.value)
        assertThat(logic.getPinnedMessages(7L)).isEmpty()
        assertThat(database.messageDao().getMessageByTid(42L)?.messageEntity?.pinDetails).isNull()
    }

    @Test
    fun `concurrent confirmations resolve the pending pin only once`() = runTest {
        val dao = database.pinnedMessageDao()
        database.messageDao().upsertMessage(messageDb())
        dao.upsertWithMirror(pinnedEntity(syncState = PinSyncStateEntity.PendingPin.value))
        val confirm = ConfirmPinUseCase(dao)

        val responses = List(2) { async(Dispatchers.IO) { confirm(7L, 42L, 900L) } }.awaitAll()

        assertThat(responses.count { it.didFlipPendingIntent }).isEqualTo(1)
        assertThat(dao.getByTid(42L, 7L)?.serverPinId).isEqualTo(900L)
    }

    @Test
    fun `a late pin acknowledgement cannot revive a pending unpin`() = runTest {
        val dao = database.pinnedMessageDao()
        database.messageDao().upsertMessage(messageDb())
        dao.upsertWithMirror(pinnedEntity(syncState = PinSyncStateEntity.Synced.value))
        dao.markPendingUnpinWithMirror(42L, 7L, 1_000L)

        val result = ConfirmPinUseCase(dao)(7L, 42L, 900L)

        assertThat(result.didFlipPendingIntent).isFalse()
        assertThat(dao.getByTid(42L, 7L)?.syncState).isEqualTo(PinSyncStateEntity.PendingUnpin.value)
        assertThat(logic.getPinnedMessages(7L)).isEmpty()
    }

    @Test
    fun `realtime unpin removes the stored outgoing pin when the server omits tid`() = runTest {
        val dao = database.pinnedMessageDao()
        database.messageDao().upsertMessage(messageDb(messageEntity(tid = 77L)))
        dao.upsertWithMirror(pinnedEntity(messageTid = 77L, syncState = PinSyncStateEntity.Synced.value))

        logic.onPinUpdated(PinUpdateEvent.Unpinned(
            7L, listOf(sceytPinnedMessage(message = sceytMessage(id = 42L, tid = 0L)))
        ))

        assertThat(dao.getByTid(77L, 7L)).isNull()
        assertThat(database.messageDao().getMessageByTid(77L)?.messageEntity?.pinDetails).isNull()
    }

    @Test
    fun `reconnect during an in-flight pin sends and announces it only once`() = runTest {
        database.messageDao().upsertMessage(messageDb())
        val requestStarted = CompletableDeferred<Unit>()
        val releaseResponse = CompletableDeferred<Unit>()
        doSuspendableAnswer {
            requestStarted.complete(Unit)
            releaseResponse.await()
            SceytResponse.Success(listOf(sceytPinnedMessage()))
        }.whenever(pinRepository) { pinMessages(any(), any(), any(), anyOrNull()) }

        val pin = launch { logic.pinMessage(7L, 42L, PinType.SHARED) }
        requestStarted.await()
        val reconnect = launch { logic.sendAllPendingPins() }
        releaseResponse.complete(Unit)
        pin.join()
        reconnect.join()

        verify(pinRepository, times(1)).pinMessages(any(), any(), any(), anyOrNull())
        verify(systemMessageSender, times(1)).sendMessagePinned(7L, 42L)
        assertThat(logic.getPinnedMessages(7L)).hasSize(1)
    }
}
