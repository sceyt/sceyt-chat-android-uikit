package com.sceyt.chatuikit.persistence.logicimpl

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_PIN_TABLE
import com.sceyt.chatuikit.persistence.logicimpl.usecases.pendingPin
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.persistence.logicimpl.usecases.PinMessageUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.RefreshPinnedMessageCacheUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendPendingPinsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendPinUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendUnpinUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.StorePinsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SyncPinnedMessagesManager
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UnpinMessageUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UpdatePinnedMessagesUseCase
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
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.mockito.MockedStatic
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockStatic
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PinnedMessagesPersistenceTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var chatClientMock: MockedStatic<ChatClient>
    private lateinit var database: SceytDatabase
    private lateinit var dependencies: KoinApplication
    private val pinRepository = mock<PinRepository>()
    private val cache = MessagesCache()
    private lateinit var refreshCache: RefreshPinnedMessageCacheUseCase
    private val systemMessageSender = mock<SystemMessageSender>()
    private lateinit var logic: PersistencePinLogicImpl

    @Before
    fun setUp() {
        val client = mock<ChatClient>()
        chatClientMock = mockStatic()
        chatClientMock.`when`<ChatClient> { ChatClient.getClient() }.thenReturn(client)
        chatClientMock.`when`<Boolean> { ChatClient.isInitialized() }.thenReturn(true)
        whenever(client.connectionState()).thenReturn(ConnectionState.Connected)
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(), SceytDatabase::class.java
        ).allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        val dao = database.pinnedMessageDao()
        val pendingDao = database.pendingPinDao()
        val messages = database.messageDao()
        dependencies = koinApplication {
            modules(useCaseModule, module {
                factory { RefreshPinnedMessageCacheUseCase(get(), get(), dispatcher) }
                factory { PinMessageUseCase(get(), get(), get(), dispatcher) }
                factory { UnpinMessageUseCase(get(), get(), get(), dispatcher) }
                factory { StorePinsUseCase(get(), get(), get(), get(), dispatcher) }
                factory { SendPendingPinsUseCase(get(), get(), get(), dispatcher) }
                factory { SendPinUseCase(get(), get(), get(), get(), get(), dispatcher) }
                factory { SendUnpinUseCase(get(), get(), get(), dispatcher) }
                factory { SyncPinnedMessagesManager(get(), get(), get(), get(), dispatcher) }
                factory { UpdatePinnedMessagesUseCase(get(), get(), get(), get(), dispatcher) }
                single { dao }
                single { pendingDao }
                single { messages }
                single { database.userDao() }
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
        chatClientMock.close()
    }

    @Test
    fun `sync stores an uncached pin sender and reply parent`() = runTest(dispatcher) {
        val sender = SceytUser("sender").copy(
            firstName = "Alice",
            avatarURL = "https://example.com/alice.png",
            metadataMap = mapOf("role" to "member"),
        )
        val parentSender = SceytUser("parent-sender").copy(
            firstName = "Bob",
            avatarURL = "https://example.com/bob.png",
        )
        val parent = sceytMessage(id = 100L, incoming = true).copy(
            body = "reply preview",
            user = parentSender,
        )
        val fetched = sceytMessage(incoming = true).copy(user = sender, parentMessage = parent)
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(
            flowOf(SceytPagingResponse.Success(listOf(sceytPinnedMessage(message = fetched)), hasNext = false))
        )

        logic.syncChannelPins(7L)

        val stored = logic.getPinnedMessagesFlow(7L).first().single().message
        assertThat(stored.user?.id).isEqualTo(sender.id)
        assertThat(stored.user?.firstName).isEqualTo(sender.firstName)
        assertThat(stored.user?.avatarURL).isEqualTo(sender.avatarURL)
        assertThat(stored.user?.metadataMap).isEqualTo(sender.metadataMap)
        assertThat(stored.parentMessage?.id).isEqualTo(parent.id)
        assertThat(stored.parentMessage?.body).isEqualTo(parent.body)
        assertThat(stored.parentMessage?.user?.firstName).isEqualTo(parentSender.firstName)
        assertThat(stored.parentMessage?.user?.avatarURL).isEqualTo(parentSender.avatarURL)
        assertThat(database.messageDao().getMessageById(fetched.id)?.messageEntity?.unList).isTrue()
        assertThat(database.messageDao().getMessageById(parent.id)?.messageEntity?.unList).isTrue()
    }

    @Test
    fun `sync preserves a reply parent already in the local timeline`() = runTest(dispatcher) {
        val localParent = messageEntity(id = 100L, tid = 88L).copy(
            body = "newer parent",
            updatedAt = 300L,
        )
        database.messageDao().upsertMessage(messageDb(localParent))
        val parent = sceytMessage(id = 100L, incoming = true).copy(body = "older parent", updatedAt = 200L)
        val fetched = sceytMessage(incoming = true).copy(parentMessage = parent)
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(
            flowOf(SceytPagingResponse.Success(listOf(sceytPinnedMessage(message = fetched)), hasNext = false))
        )

        logic.syncChannelPins(7L)

        val stored = logic.getPinnedMessagesFlow(7L).first().single().message.parentMessage
        assertThat(stored?.tid).isEqualTo(localParent.tid)
        assertThat(stored?.body).isEqualTo(localParent.body)
        val storedParent = database.messageDao().getMessageById(parent.id)?.messageEntity
        assertThat(storedParent?.updatedAt).isEqualTo(localParent.updatedAt)
        assertThat(storedParent?.unList).isFalse()
    }

    @Test
    fun `sync restores missing relations when the cached message is already current`() = runTest(dispatcher) {
        val sender = SceytUser("sender").copy(firstName = "Alice")
        val parent = sceytMessage(id = 100L, incoming = true).copy(body = "reply preview")
        val local = messageEntity(tid = 77L).copy(
            fromId = sender.id,
            parentId = parent.id,
            updatedAt = 200L,
        )
        database.messageDao().upsertMessage(messageDb(local))
        val fetched = sceytMessage(tid = 0L).copy(
            user = sender,
            parentMessage = parent,
            updatedAt = local.updatedAt,
        )
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(
            flowOf(SceytPagingResponse.Success(listOf(sceytPinnedMessage(message = fetched)), hasNext = false))
        )

        logic.syncChannelPins(7L)

        val stored = logic.getPinnedMessagesFlow(7L).first().single().message
        assertThat(stored.tid).isEqualTo(local.tid)
        assertThat(stored.user?.firstName).isEqualTo(sender.firstName)
        assertThat(stored.parentMessage?.id).isEqualTo(parent.id)
        assertThat(stored.parentMessage?.body).isEqualTo(parent.body)
    }

    @Test
    fun `sync refreshes an edited pin while preserving its local tid and list visibility`() = runTest(dispatcher) {
        val local = messageEntity(tid = 77L).copy(body = "old body", updatedAt = 100L)
        database.messageDao().upsertMessage(messageDb(local))
        cache.add(7L, sceytMessage(tid = 77L).copy(body = "old body", updatedAt = 100L))
        val fetched = sceytMessage(tid = 0L).copy(body = "edited body", updatedAt = 200L)
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(
            flowOf(SceytPagingResponse.Success(listOf(sceytPinnedMessage(message = fetched)), hasNext = false))
        )

        logic.syncChannelPins(7L)

        val stored = database.messageDao().getMessageById(42L)!!.messageEntity
        assertThat(stored.body).isEqualTo("edited body")
        assertThat(stored.updatedAt).isEqualTo(200L)
        assertThat(stored.tid).isEqualTo(77L)
        assertThat(stored.unList).isFalse()
        assertThat(cache.get(7L, 77L)?.body).isEqualTo("edited body")
        assertThat(logic.getPinnedMessagesFlow(7L).first().single().message.body).isEqualTo("edited body")
    }

    @Test
    fun `sync does not overwrite a newer local message with an older pin response`() = runTest(dispatcher) {
        database.messageDao().upsertMessage(messageDb(
            messageEntity(tid = 77L).copy(body = "newer body", updatedAt = 300L, unList = true)
        ))
        val fetched = sceytMessage(tid = 0L).copy(body = "stale body", updatedAt = 200L)
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(
            flowOf(SceytPagingResponse.Success(listOf(sceytPinnedMessage(message = fetched)), hasNext = false))
        )

        logic.syncChannelPins(7L)

        val stored = database.messageDao().getMessageById(42L)!!.messageEntity
        assertThat(stored.body).isEqualTo("newer body")
        assertThat(stored.updatedAt).isEqualTo(300L)
        assertThat(stored.tid).isEqualTo(77L)
        assertThat(stored.unList).isTrue()
        assertThat(database.pinnedMessageDao().getByTid(77L)).isNotNull()
    }

    @Test
    fun `failed unpin followed by successful sync preserves removal intent`() = runTest(dispatcher) {
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
    fun `realtime unpin removes the stored outgoing pin when the server omits tid`() = runTest(dispatcher) {
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
    fun `realtime pin survives a sync response fetched before that pin`() = runTest(dispatcher) {
        database.messageDao().upsertMessage(messageDb())
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(kotlinx.coroutines.flow.flow {
            started.complete(Unit)
            release.await()
            emit(SceytPagingResponse.Success(emptyList(), hasNext = false))
        })

        val sync = launch { logic.syncChannelPins(7L) }
        started.await()
        val pin = launch(start = CoroutineStart.UNDISPATCHED) {
            logic.onPinUpdated(PinUpdateEvent.Pinned(7L, listOf(sceytPinnedMessage())))
        }
        release.complete(Unit)
        sync.join()
        pin.join()

        assertThat(database.pinnedMessageDao().getByTid(42L)).isNotNull()
        assertThat(logic.getPinnedMessagesFlow(7L).first()).hasSize(1)
    }

    @Test
    fun `realtime unpin survives a sync response fetched before that unpin`() = runTest(dispatcher) {
        database.messageDao().upsertMessage(messageDb())
        database.pinnedMessageDao().insert(pinnedEntity())
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(kotlinx.coroutines.flow.flow {
            started.complete(Unit)
            release.await()
            emit(SceytPagingResponse.Success(listOf(sceytPinnedMessage()), hasNext = false))
        })

        val sync = launch { logic.syncChannelPins(7L) }
        started.await()
        val unpin = launch(start = CoroutineStart.UNDISPATCHED) {
            logic.onPinUpdated(PinUpdateEvent.Unpinned(7L, listOf(sceytPinnedMessage())))
        }
        release.complete(Unit)
        sync.join()
        unpin.join()

        assertThat(database.pinnedMessageDao().getByTid(42L)).isNull()
        assertThat(logic.getPinnedMessagesFlow(7L).first()).isEmpty()
    }

    @Test
    fun `pin then unpin queued during sync leaves the message unpinned`() = runTest(dispatcher) {
        database.messageDao().upsertMessage(messageDb())
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(kotlinx.coroutines.flow.flow {
            started.complete(Unit)
            release.await()
            emit(SceytPagingResponse.Success(listOf(sceytPinnedMessage()), hasNext = false))
        })

        val sync = launch { logic.syncChannelPins(7L) }
        started.await()
        val pin = launch(start = CoroutineStart.UNDISPATCHED) {
            logic.onPinUpdated(PinUpdateEvent.Pinned(7L, listOf(sceytPinnedMessage())))
        }
        val unpin = launch(start = CoroutineStart.UNDISPATCHED) {
            logic.onPinUpdated(PinUpdateEvent.Unpinned(7L, listOf(sceytPinnedMessage())))
        }
        release.complete(Unit)
        sync.join()
        pin.join()
        unpin.join()

        assertThat(database.pinnedMessageDao().getByTid(42L)).isNull()
    }

    @Test
    fun `reconnect during an in-flight pin sends and announces it only once`() = runTest(dispatcher) {
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
    fun `unpin refreshes the bubble before a stalled pin request returns`() = runTest(dispatcher) {
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
    fun `repin refreshes the bubble before a stalled unpin returns`() = runTest(dispatcher) {
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
    fun `pin refreshes the bubble while channel sync is stalled`() = runTest(dispatcher) {
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
    fun `late rejection of an old pin cannot remove a newer pin intent`() = runTest(dispatcher) {
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
    fun `pin returns the matching confirmed server pin`() = runTest(dispatcher) {
        database.messageDao().upsertMessage(messageDb())
        val confirmed = sceytPinnedMessage()
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenReturn(SceytResponse.Success(listOf(confirmed)))

        val result = logic.pinMessage(7L, 42L, PinType.SHARED)

        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        assertThat((result as SceytResponse.Success).data).isEqualTo(confirmed)
    }

    @Test
    fun `pin returns nullable success when the server has no matching pin`() = runTest(dispatcher) {
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
    fun `rejected repin after successful unpin leaves no pin`() = runTest(dispatcher) {
        val rejection = mock<com.sceyt.chat.models.SceytException> {
            on { type }.thenReturn("NotAllowed")
        }
        checkRepinAfterUnpin(SceytResponse.Error(rejection), confirmed = false, pending = false)
    }

    @Test
    fun `retryable repin after successful unpin keeps only pending intent`() = runTest(dispatcher) {
        checkRepinAfterUnpin(SceytResponse.Error(null), confirmed = false, pending = true)
    }

    @Test
    fun `successful repin replaces the removed confirmed pin`() = runTest(dispatcher) {
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
    fun `settling an old unpin removes confirmed state but preserves the newer request id`() = runTest(dispatcher) {
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
    fun `failed settlement rolls back confirmed deletion and retains the pending unpin`() = runTest(dispatcher) {
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
