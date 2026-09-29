package com.sceyt.chatuikit.persistence.logicimpl

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_PIN_TABLE
import com.sceyt.chatuikit.persistence.logicimpl.usecases.pendingPin
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.persistence.logicimpl.usecases.RefreshPinnedMessageCacheUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.messageDb
import com.sceyt.chatuikit.persistence.logicimpl.usecases.messageEntity
import com.sceyt.chatuikit.persistence.logicimpl.usecases.pinnedEntity
import com.sceyt.chatuikit.persistence.logicimpl.usecases.sceytMessage
import com.sceyt.chatuikit.persistence.logicimpl.usecases.sceytPinnedMessage
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineStart
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import com.sceyt.chatuikit.persistence.di.useCaseModule
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
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
    private lateinit var dependencies: KoinApplication
    private val pinRepository = mock<PinRepository>()
    private val cache = MessagesCache()
    private lateinit var refreshCache: RefreshPinnedMessageCacheUseCase
    private val systemMessageSender = mock<SystemMessageSender>()
    private lateinit var logic: PersistencePinLogicImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(), SceytDatabase::class.java
        ).allowMainThreadQueries().build()
        val dao = database.pinnedMessageDao()
        val pendingDao = database.pendingPinDao()
        val messages = database.messageDao()
        dependencies = koinApplication {
            modules(useCaseModule, module {
                single { dao }
                single { pendingDao }
                single { messages }
                single { cache }
                single { pinRepository }
                single { systemMessageSender }
            })
        }
        val koin = dependencies.koin
        refreshCache = koin.get()
        logic = PersistencePinLogicImpl(
            dao, pendingDao, koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get()
        )
    }

    @After
    fun tearDown() {
        dependencies.close()
        database.close()
    }

    @Test
    fun `failed unpin followed by successful sync preserves removal intent`() = runTest {
        val dao = database.pinnedMessageDao()
        database.messageDao().upsertMessage(messageDb())
        dao.insert(pinnedEntity())
        whenever(pinRepository.unpinMessages(any(), any())).thenReturn(SceytResponse.Error(null))
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(
            flowOf(SceytPagingResponse.Success(listOf(sceytPinnedMessage()), hasNext = false))
        )

        logic.unpinMessage(7L, 42L)
        logic.syncChannelPins(7L)

        assertThat(database.pendingPinDao().getByTid(42L)?.isPin).isFalse()
        assertThat(logic.getPinnedMessagesFlow(7L).first()).isEmpty()
        assertThat(database.messageDao().getMessageByTid(42L)?.toSceytMessage()?.pinDetails).isNull()
    }

    @Test
    fun `realtime unpin removes the stored outgoing pin when the server omits tid`() = runTest {
        val dao = database.pinnedMessageDao()
        database.messageDao().upsertMessage(messageDb(messageEntity(tid = 77L)))
        dao.insert(pinnedEntity(messageTid = 77L))

        logic.onPinUpdated(PinUpdateEvent.Unpinned(
            7L, listOf(sceytPinnedMessage(message = sceytMessage(id = 42L, tid = 0L)))
        ))

        assertThat(dao.getByTid(77L)).isNull()
        assertThat(database.messageDao().getMessageByTid(77L)?.toSceytMessage()?.pinDetails).isNull()
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
        verify(systemMessageSender, times(1)).sendMessagePinned(eq(7L), argThat { id == 42L })
        assertThat(logic.getPinnedMessagesFlow(7L).first()).hasSize(1)
    }

    @Test
    fun `unpin refreshes the bubble before a stalled pin request returns`() = runTest {
        database.messageDao().upsertMessage(messageDb())
        cache.add(7L, sceytMessage())
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        doSuspendableAnswer {
            started.complete(Unit)
            release.await()
            SceytResponse.Success(listOf(sceytPinnedMessage()))
        }.whenever(pinRepository) { pinMessages(any(), any(), any(), anyOrNull()) }
        whenever(pinRepository.unpinMessages(any(), any())).thenReturn(SceytResponse.Error(null))

        val pin = async { logic.pinMessage(7L, 42L, PinType.SHARED) }
        started.await()
        assertThat(cache.get(7L, 42L)?.pinDetails?.isPinned).isTrue()
        val hidden = async(start = CoroutineStart.UNDISPATCHED) {
            MessagesCache.messageUpdatedFlow.first { (_, messages) ->
                messages.any { it.tid == 42L && it.pinDetails == null }
            }
        }
        val unpin = launch { logic.unpinMessage(7L, 42L) }
        hidden.await()
        assertThat(cache.get(7L, 42L)?.pinDetails).isNull()

        release.complete(Unit)
        val result = pin.await()
        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        assertThat((result as SceytResponse.Success).data).isNull()
        unpin.join()
        // The pin landed on the server, but the unpin that replaced it is still pending.
        assertThat(cache.get(7L, 42L)?.pinDetails).isNull()
        assertThat(database.pendingPinDao().getByTid(42L)?.isPin).isFalse()
        verify(systemMessageSender, org.mockito.kotlin.never()).sendMessagePinned(any(), any())
    }

    @Test
    fun `repin refreshes the bubble before a stalled unpin returns`() = runTest {
        database.messageDao().upsertMessage(messageDb())
        database.pinnedMessageDao().insert(pinnedEntity())
        cache.add(7L, sceytMessage())
        refreshCache(7L, 42L)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        doSuspendableAnswer {
            started.complete(Unit)
            release.await()
            SceytResponse.Success(emptyList<com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage>())
        }.whenever(pinRepository) { unpinMessages(any(), any()) }
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenReturn(SceytResponse.Error(null))

        val unpin = launch { logic.unpinMessage(7L, 42L) }
        started.await()
        assertThat(cache.get(7L, 42L)?.pinDetails).isNull()
        val shown = async(start = CoroutineStart.UNDISPATCHED) {
            MessagesCache.messageUpdatedFlow.first { (_, messages) ->
                messages.any { it.tid == 42L && it.pinDetails?.isPinned == true }
            }
        }
        val pin = launch { logic.pinMessage(7L, 42L, PinType.SHARED) }
        shown.await()
        assertThat(cache.get(7L, 42L)?.pinDetails?.isPinned).isTrue()

        release.complete(Unit)
        unpin.join()
        pin.join()
        assertThat(cache.get(7L, 42L)?.pinDetails?.isPinned).isTrue()
        assertThat(database.pendingPinDao().getByTid(42L)?.isPin).isTrue()
    }

    @Test
    fun `pin refreshes the bubble while channel sync is stalled`() = runTest {
        database.messageDao().upsertMessage(messageDb())
        cache.add(7L, sceytMessage())
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(kotlinx.coroutines.flow.flow {
            started.complete(Unit)
            release.await()
            emit(SceytPagingResponse.Success(emptyList(), hasNext = false))
        })
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenReturn(SceytResponse.Error(null))
        val sync = launch { logic.syncChannelPins(7L) }
        started.await()
        val shown = async(start = CoroutineStart.UNDISPATCHED) {
            MessagesCache.messageUpdatedFlow.first { (_, messages) ->
                messages.any { it.tid == 42L && it.pinDetails?.isPinned == true }
            }
        }
        val pin = launch { logic.pinMessage(7L, 42L, PinType.SHARED) }
        shown.await()
        assertThat(cache.get(7L, 42L)?.pinDetails?.isPinned).isTrue()
        release.complete(Unit)
        sync.join()
        pin.join()
        assertThat(logic.getPinnedMessagesFlow(7L).first()).hasSize(1)
    }

    @Test
    fun `late rejection of an old pin cannot remove a newer pin intent`() = runTest {
        database.messageDao().upsertMessage(messageDb())
        cache.add(7L, sceytMessage())
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val rejection = mock<com.sceyt.chat.models.SceytException> {
            on { type }.thenReturn("NotAllowed")
        }
        var calls = 0
        doSuspendableAnswer {
            if (calls++ == 0) {
                started.complete(Unit)
                release.await()
                SceytResponse.Error<List<com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage>>(rejection)
            } else SceytResponse.Success(listOf(sceytPinnedMessage()))
        }.whenever(pinRepository) { pinMessages(any(), any(), any(), anyOrNull()) }
        val firstPin = launch { logic.pinMessage(7L, 42L, PinType.SHARED) }
        started.await()
        val hidden = async(start = CoroutineStart.UNDISPATCHED) {
            MessagesCache.messageUpdatedFlow.first { (_, messages) ->
                messages.any { it.tid == 42L && it.pinDetails == null }
            }
        }
        val unpin = launch { logic.unpinMessage(7L, 42L) }
        hidden.await()
        val shown = async(start = CoroutineStart.UNDISPATCHED) {
            MessagesCache.messageUpdatedFlow.first { (_, messages) ->
                messages.any { it.tid == 42L && it.pinDetails?.isPinned == true }
            }
        }
        val lastPin = launch { logic.pinMessage(7L, 42L, PinType.SHARED) }
        shown.await()
        release.complete(Unit)
        firstPin.join()
        unpin.join()
        lastPin.join()

        assertThat(cache.get(7L, 42L)?.pinDetails?.isPinned).isTrue()
        assertThat(database.pinnedMessageDao().getByTid(42L)).isNotNull()
        assertThat(database.pendingPinDao().getByTid(42L)).isNull()
        verify(systemMessageSender, times(1)).sendMessagePinned(eq(7L), argThat { id == 42L })
        verify(pinRepository, org.mockito.kotlin.never()).unpinMessages(any(), any())
    }


    @Test
    fun `pin returns the matching confirmed server pin`() = runTest {
        database.messageDao().upsertMessage(messageDb())
        val confirmed = sceytPinnedMessage()
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenReturn(SceytResponse.Success(listOf(confirmed)))

        val result = logic.pinMessage(7L, 42L, PinType.SHARED)

        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        assertThat((result as SceytResponse.Success).data).isEqualTo(confirmed)
    }

    @Test
    fun `pin returns nullable success when the server has no matching pin`() = runTest {
        database.messageDao().upsertMessage(messageDb())
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenReturn(SceytResponse.Success(listOf(
                sceytPinnedMessage(message = sceytMessage(id = 999L, tid = 999L))
            )))

        val result = logic.pinMessage(7L, 42L, PinType.SHARED)

        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        assertThat((result as SceytResponse.Success).data).isNull()
    }

    @Test
    fun `rejected repin after successful unpin leaves no pin`() = runTest {
        val rejection = mock<com.sceyt.chat.models.SceytException> {
            on { type }.thenReturn("NotAllowed")
        }
        checkRepinAfterUnpin(SceytResponse.Error(rejection), confirmed = false, pending = false)
    }

    @Test
    fun `retryable repin after successful unpin keeps only pending intent`() = runTest {
        checkRepinAfterUnpin(SceytResponse.Error(null), confirmed = false, pending = true)
    }

    @Test
    fun `successful repin replaces the removed confirmed pin`() = runTest {
        checkRepinAfterUnpin(SceytResponse.Success(listOf(sceytPinnedMessage())), confirmed = true, pending = false)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.checkRepinAfterUnpin(
        response: SceytResponse<List<com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage>>,
        confirmed: Boolean,
        pending: Boolean,
    ) {
        database.messageDao().upsertMessage(messageDb())
        database.pinnedMessageDao().insert(pinnedEntity())
        cache.add(7L, sceytMessage())
        refreshCache(7L, 42L)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        doSuspendableAnswer {
            started.complete(Unit)
            release.await()
            SceytResponse.Success(emptyList<com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage>())
        }.whenever(pinRepository) { unpinMessages(any(), any()) }
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull())).thenReturn(response)

        val unpin = launch { logic.unpinMessage(7L, 42L) }
        started.await()
        val shown = async(start = CoroutineStart.UNDISPATCHED) {
            MessagesCache.messageUpdatedFlow.first { (_, messages) ->
                messages.any { it.tid == 42L && it.pinDetails?.isPinned == true }
            }
        }
        val repin = launch { logic.pinMessage(7L, 42L, PinType.SHARED) }
        shown.await()
        release.complete(Unit)
        unpin.join()
        repin.join()

        assertThat(database.pinnedMessageDao().getByTid(42L) != null).isEqualTo(confirmed)
        assertThat(database.pendingPinDao().getByTid(42L)?.isPin == true).isEqualTo(pending)
        assertThat(logic.getPinnedMessagesFlow(7L).first()).hasSize(if (confirmed || pending) 1 else 0)
        assertThat(cache.get(7L, 42L)?.pinDetails?.isPinned == true).isEqualTo(confirmed || pending)
    }

    @Test
    fun `settling an old unpin removes confirmed state but preserves the newer request id`() = runTest {
        database.messageDao().upsertMessage(messageDb())
        database.pinnedMessageDao().insert(pinnedEntity())
        val dao = database.pendingPinDao()
        val sent = dao.replace(pendingPin(messageTid = 42L, isPin = false))!!
        val newer = dao.replace(pendingPin(messageTid = 42L, isPin = true))!!

        dao.settleUnpin(sent)

        assertThat(database.pinnedMessageDao().getByTid(42L)).isNull()
        assertThat(dao.getByTid(42L)).isEqualTo(newer)
        assertThat(logic.getPinnedMessagesFlow(7L).first()).hasSize(1)
    }

    @Test
    fun `failed settlement rolls back confirmed deletion and retains the pending unpin`() = runTest {
        database.messageDao().upsertMessage(messageDb())
        database.pinnedMessageDao().insert(pinnedEntity())
        val dao = database.pendingPinDao()
        val sent = dao.replace(pendingPin(messageTid = 42L, isPin = false))!!
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_pending_delete BEFORE DELETE ON $PENDING_PIN_TABLE " +
                "BEGIN SELECT RAISE(ABORT, 'test settlement failure'); END"
        )

        val result = runCatching { dao.settleUnpin(sent) }

        assertThat(result.isFailure).isTrue()
        assertThat(database.pinnedMessageDao().getByTid(42L)).isNotNull()
        assertThat(dao.getByTid(42L)).isEqualTo(sent)
        assertThat(logic.getPinnedMessagesFlow(7L).first()).isEmpty()
    }

}
