package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelDb
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.logicimpl.usecases.CreatePendingChannelUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.FindExistingChannelByMembersUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.FindRealChannelForPendingUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.InsertChannelWithMembersUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.MergePendingDirectChannelsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.MigratePendingChannelToRealChannelUseCase
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

class PendingChannelCoordinatorTest {

    private companion object {
        const val CURRENT_USER_ID = "current-user"
        const val PENDING_CHANNEL_ID = 10L
        const val REAL_CHANNEL_ID = 20L
        const val CHANNEL_URI = "shared-uri"
    }

    private val channelsRepository = mock<ChannelsRepository>()
    private val channelDao = mock<ChannelDao>()
    private val channelsCache = mock<ChannelsCache>()
    private val findExistingChannelByMembersUseCase = mock<FindExistingChannelByMembersUseCase>()
    private val createPendingChannelUseCase = mock<CreatePendingChannelUseCase>()
    private val findRealChannelForPendingUseCase = mock<FindRealChannelForPendingUseCase>()
    private val migratePendingChannelToRealChannelUseCase = mock<MigratePendingChannelToRealChannelUseCase>()
    private val mergePendingDirectChannelsUseCase = mock<MergePendingDirectChannelsUseCase>()
    private val insertChannelWithMembersUseCase = mock<InsertChannelWithMembersUseCase>()
    private val coordinator = PendingChannelCoordinator(
        channelsRepository = channelsRepository,
        channelDao = channelDao,
        channelsCache = channelsCache,
        findExistingChannelByMembersUseCase = findExistingChannelByMembersUseCase,
        createPendingChannelUseCase = createPendingChannelUseCase,
        findRealChannelForPendingUseCase = findRealChannelForPendingUseCase,
        migratePendingChannelToRealChannelUseCase = migratePendingChannelToRealChannelUseCase,
        mergePendingDirectChannelsUseCase = mergePendingDirectChannelsUseCase,
        insertChannelWithMembersUseCase = insertChannelWithMembersUseCase,
    )

    // region createRealFromPending

    @Test
    fun `snapshots pending channel before persisting same uri create response`() = runTest {
        val pendingChannel = pendingChannel()
        val realChannel = realChannel()
        val mergedChannel = realChannel.copy(subject = "merged")
        val createdChannelPersisted = AtomicBoolean(false)
        whenever(findRealChannelForPendingUseCase(pendingChannel, CURRENT_USER_ID)).thenReturn(null)
        whenever(channelsRepository.createChannel(any())).thenReturn(SceytResponse.Success(realChannel))
        whenever(channelDao.getChannelById(PENDING_CHANNEL_ID)).thenAnswer {
            if (createdChannelPersisted.get()) null else pendingChannel.toChannelDb()
        }
        doSuspendableAnswer {
            createdChannelPersisted.set(true)
        }.whenever(insertChannelWithMembersUseCase) { invoke(any(), any()) }
        whenever(migratePendingChannelToRealChannelUseCase(any(), eq(realChannel)))
            .thenReturn(mergedChannel)

        val response = coordinator.createRealFromPending(pendingChannel, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(mergedChannel)
        verifyBlocking(migratePendingChannelToRealChannelUseCase) {
            invoke(
                check { snapshot ->
                    assertThat(snapshot.id).isEqualTo(PENDING_CHANNEL_ID)
                    assertThat(snapshot.pending).isTrue()
                    assertThat(snapshot.uri).isEqualTo(CHANNEL_URI)
                },
                eq(realChannel)
            )
        }
    }

    @Test
    fun `uses real channel found before remote create`() = runTest {
        val pendingChannel = pendingChannel()
        val realChannel = realChannel()
        val mergedChannel = realChannel.copy(subject = "already merged")
        whenever(findRealChannelForPendingUseCase(pendingChannel, CURRENT_USER_ID)).thenReturn(realChannel)
        whenever(channelDao.getChannelById(PENDING_CHANNEL_ID)).thenReturn(pendingChannel.toChannelDb())
        whenever(migratePendingChannelToRealChannelUseCase(any(), eq(realChannel)))
            .thenReturn(mergedChannel)

        val response = coordinator.createRealFromPending(pendingChannel, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(mergedChannel)
        verifyBlocking(insertChannelWithMembersUseCase, never()) { invoke(any(), any()) }
        verifyBlocking(channelsRepository, never()) { createChannel(any()) }
    }

    @Test
    fun `returns real channel as is when local pending row is already gone`() = runTest {
        val pendingChannel = pendingChannel()
        val realChannel = realChannel()
        whenever(findRealChannelForPendingUseCase(pendingChannel, CURRENT_USER_ID)).thenReturn(realChannel)
        whenever(channelDao.getChannelById(PENDING_CHANNEL_ID)).thenReturn(null)

        val response = coordinator.createRealFromPending(pendingChannel, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(realChannel)
        verifyBlocking(migratePendingChannelToRealChannelUseCase, never()) { invoke(any(), any()) }
        verifyBlocking(channelsRepository, never()) { createChannel(any()) }
    }

    @Test
    fun `uses real channel found while remote create is in flight`() = runTest {
        val pendingChannel = pendingChannel()
        val realChannel = realChannel()
        val duplicateCreateResponse = realChannel.copy(id = 30L)
        val mergedChannel = realChannel.copy(subject = "sync won")
        whenever(findRealChannelForPendingUseCase(pendingChannel, CURRENT_USER_ID))
            .thenReturn(null, realChannel)
        whenever(channelsRepository.createChannel(any()))
            .thenReturn(SceytResponse.Success(duplicateCreateResponse))
        whenever(channelDao.getChannelById(PENDING_CHANNEL_ID)).thenReturn(pendingChannel.toChannelDb())
        whenever(migratePendingChannelToRealChannelUseCase(any(), eq(realChannel)))
            .thenReturn(mergedChannel)

        val response = coordinator.createRealFromPending(pendingChannel, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(mergedChannel)
        verifyBlocking(insertChannelWithMembersUseCase, never()) { invoke(any(), any()) }
        verifyBlocking(migratePendingChannelToRealChannelUseCase, never()) {
            invoke(any(), eq(duplicateCreateResponse))
        }
    }

    @Test
    fun `maps create channel data from the pending channel`() = runTest {
        val pendingChannel = pendingChannel().copy(subject = "the subject", metadata = "{}")
        whenever(findRealChannelForPendingUseCase(pendingChannel, CURRENT_USER_ID)).thenReturn(null)
        whenever(channelsRepository.createChannel(any()))
            .thenReturn(SceytResponse.Success(realChannel()))
        whenever(channelDao.getChannelById(PENDING_CHANNEL_ID)).thenReturn(null)

        coordinator.createRealFromPending(pendingChannel, CURRENT_USER_ID)

        verifyBlocking(channelsRepository) {
            createChannel(check { data ->
                assertThat(data.type).isEqualTo(pendingChannel.type)
                assertThat(data.uri).isEqualTo(CHANNEL_URI)
                assertThat(data.subject).isEqualTo("the subject")
                assertThat(data.metadata).isEqualTo("{}")
            })
        }
    }

    @Test
    fun `propagates remote create error`() = runTest {
        val pendingChannel = pendingChannel()
        val exception = SceytException(42, "boom")
        whenever(findRealChannelForPendingUseCase(pendingChannel, CURRENT_USER_ID)).thenReturn(null)
        whenever(channelsRepository.createChannel(any()))
            .thenReturn(SceytResponse.Error(exception))

        val response = coordinator.createRealFromPending(pendingChannel, CURRENT_USER_ID)

        assertThat(response).isInstanceOf(SceytResponse.Error::class.java)
        assertThat((response as SceytResponse.Error).exception).isEqualTo(exception)
        verifyBlocking(insertChannelWithMembersUseCase, never()) { invoke(any(), any()) }
    }

    @Test
    fun `errors when remote create succeeds with null channel`() = runTest {
        val pendingChannel = pendingChannel()
        whenever(findRealChannelForPendingUseCase(pendingChannel, CURRENT_USER_ID)).thenReturn(null)
        whenever(channelsRepository.createChannel(any())).thenReturn(SceytResponse.Success(null))

        val response = coordinator.createRealFromPending(pendingChannel, CURRENT_USER_ID)

        assertThat(response).isInstanceOf(SceytResponse.Error::class.java)
        verifyBlocking(insertChannelWithMembersUseCase, never()) { invoke(any(), any()) }
    }

    @Test
    fun `mutex serializes concurrent create response reconciliation`() = runBlocking {
        val pendingChannel = pendingChannel()
        val realChannel = realChannel()
        val mergedChannel = realChannel.copy(subject = "merged once")
        val createCallCount = AtomicInteger()
        val findRealCallCount = AtomicInteger()
        val persistCallCount = AtomicInteger()
        val migrationCallCount = AtomicInteger()
        val pendingChannelPresent = AtomicBoolean(true)
        val migratedRealChannel = AtomicReference<SceytChannel?>()
        val bothCreateRequestsStarted = CompletableDeferred<Unit>()
        val releaseCreateResponses = CompletableDeferred<Unit>()
        val firstPersistStarted = CompletableDeferred<Unit>()
        val releaseFirstPersist = CompletableDeferred<Unit>()
        val secondPostCreateCheckStarted = CompletableDeferred<Unit>()

        doSuspendableAnswer {
            if (findRealCallCount.incrementAndGet() == 4)
                secondPostCreateCheckStarted.complete(Unit)
            migratedRealChannel.get()
        }.whenever(findRealChannelForPendingUseCase) {
            invoke(pendingChannel, CURRENT_USER_ID)
        }
        doSuspendableAnswer {
            if (createCallCount.incrementAndGet() == 2)
                bothCreateRequestsStarted.complete(Unit)
            releaseCreateResponses.await()
            SceytResponse.Success(realChannel)
        }.whenever(channelsRepository) { createChannel(any()) }
        whenever(channelDao.getChannelById(PENDING_CHANNEL_ID)).thenAnswer {
            if (pendingChannelPresent.get()) pendingChannel.toChannelDb() else null
        }
        doSuspendableAnswer {
            migrationCallCount.incrementAndGet()
            migratedRealChannel.set(mergedChannel)
            mergedChannel
        }.whenever(migratePendingChannelToRealChannelUseCase) { invoke(any(), eq(realChannel)) }

        doSuspendableAnswer {
            persistCallCount.incrementAndGet()
            pendingChannelPresent.set(false)
            firstPersistStarted.complete(Unit)
            releaseFirstPersist.await()
        }.whenever(insertChannelWithMembersUseCase) { invoke(any(), any()) }

        val responses = List(2) {
            async(Dispatchers.Default) {
                coordinator.createRealFromPending(pendingChannel, CURRENT_USER_ID)
            }
        }
        withTimeout(1_000.milliseconds) { bothCreateRequestsStarted.await() }
        releaseCreateResponses.complete(Unit)
        withTimeout(1_000.milliseconds) { firstPersistStarted.await() }

        val enteredSecondPostCreateCheckWhileFirstHeldLock =
            withTimeoutOrNull(200.milliseconds) { secondPostCreateCheckStarted.await() }

        assertThat(enteredSecondPostCreateCheckWhileFirstHeldLock).isNull()
        assertThat(findRealCallCount.get()).isEqualTo(3)

        releaseFirstPersist.complete(Unit)
        val results = withTimeout(1_000.milliseconds) { responses.awaitAll() }

        assertThat(results.map { it.data }).containsExactly(mergedChannel, mergedChannel)
        assertThat(createCallCount.get()).isEqualTo(2)
        assertThat(persistCallCount.get()).isEqualTo(1)
        assertThat(migrationCallCount.get()).isEqualTo(1)
    }

    // endregion

    // region persistAndMergeFetchedChannels

    @Test
    fun `persists fetched channels then merges pending direct channels`() = runTest {
        val realChannel = realChannel()
        val mergedChannel = realChannel.copy(subject = "merged")
        val links = listOf(UserChatLinkEntity(userId = "peer", chatId = REAL_CHANNEL_ID, role = "member"))
        whenever(mergePendingDirectChannelsUseCase(listOf(realChannel), CURRENT_USER_ID))
            .thenReturn(listOf(mergedChannel))

        val result = coordinator.persistAndMergeFetchedChannels(listOf(realChannel), links, CURRENT_USER_ID)

        assertThat(result).containsExactly(mergedChannel)
        verifyBlocking(channelDao) {
            insertChannelsAndLinks(
                check { entities -> assertThat(entities.map { it.id }).containsExactly(REAL_CHANNEL_ID) },
                eq(links)
            )
        }
    }

    @Test
    fun `releases matching pending uri before insert and migrates its snapshot`() = runTest {
        val pendingChannel = pendingChannel()
        val realChannel = realChannel()
        val uriMergedChannel = realChannel.copy(subject = "uri merged")
        val pendingUriReleased = AtomicBoolean(false)
        whenever(channelDao.getChannelByUri(CHANNEL_URI)).thenReturn(pendingChannel.toChannelDb())
        doSuspendableAnswer {
            pendingUriReleased.set(true)
        }.whenever(channelDao) { updateUri(PENDING_CHANNEL_ID, null) }
        doSuspendableAnswer {
            assertThat(pendingUriReleased.get()).isTrue()
        }.whenever(channelDao) { insertChannelsAndLinks(any(), any()) }
        whenever(migratePendingChannelToRealChannelUseCase(pendingChannel, realChannel))
            .thenReturn(uriMergedChannel)
        whenever(mergePendingDirectChannelsUseCase(listOf(uriMergedChannel), CURRENT_USER_ID))
            .thenReturn(listOf(uriMergedChannel))

        val result = coordinator.persistAndMergeFetchedChannels(
            realChannels = listOf(realChannel),
            links = emptyList(),
            currentUserId = CURRENT_USER_ID
        )

        assertThat(result).containsExactly(uriMergedChannel)
        verifyBlocking(channelDao) { updateUri(PENDING_CHANNEL_ID, null) }
        verifyBlocking(migratePendingChannelToRealChannelUseCase) { invoke(pendingChannel, realChannel) }
    }

    @Test
    fun `still persists but skips merge when every fetched channel is pending`() = runTest {
        val pendingChannel = pendingChannel()

        val result = coordinator.persistAndMergeFetchedChannels(listOf(pendingChannel), emptyList(), CURRENT_USER_ID)

        assertThat(result).containsExactly(pendingChannel)
        verifyBlocking(channelDao) { insertChannelsAndLinks(any(), eq(emptyList())) }
        verifyBlocking(mergePendingDirectChannelsUseCase, never()) { invoke(any<List<SceytChannel>>(), any()) }
    }

    /**
     * Regression guard: the DB write and the pending merge must happen under one lock hold,
     * otherwise a concurrent [PendingChannelCoordinator.createRealFromPending] can observe the
     * freshly-written real channel before it has been merged with the pending one.
     */
    @Test
    fun `holds the mutex across the fetched channels write and merge`() = runBlocking {
        val pendingChannel = pendingChannel()
        val realChannel = realChannel()
        val insertStarted = CompletableDeferred<Unit>()
        val releaseInsert = CompletableDeferred<Unit>()
        val mergeStarted = CompletableDeferred<Unit>()
        val createReachedLock = CompletableDeferred<Unit>()

        doSuspendableAnswer {
            insertStarted.complete(Unit)
            releaseInsert.await()
        }.whenever(channelDao) { insertChannelsAndLinks(any(), any()) }
        doSuspendableAnswer {
            mergeStarted.complete(Unit)
            listOf(realChannel)
        }.whenever(mergePendingDirectChannelsUseCase) { invoke(any<List<SceytChannel>>(), any()) }
        doSuspendableAnswer {
            createReachedLock.complete(Unit)
            null
        }.whenever(findRealChannelForPendingUseCase) { invoke(any(), any()) }
        whenever(channelsRepository.createChannel(any())).thenReturn(SceytResponse.Success(realChannel))
        whenever(channelDao.getChannelById(PENDING_CHANNEL_ID)).thenReturn(null)

        val sync = async(Dispatchers.Default) {
            coordinator.persistAndMergeFetchedChannels(listOf(realChannel), emptyList(), CURRENT_USER_ID)
        }
        withTimeout(1_000.milliseconds) { insertStarted.await() }

        val create = async(Dispatchers.Default) {
            coordinator.createRealFromPending(pendingChannel, CURRENT_USER_ID)
        }

        // The write is in flight and the merge has not run yet, so nothing else may take the lock.
        assertThat(withTimeoutOrNull(200.milliseconds) { createReachedLock.await() }).isNull()
        assertThat(mergeStarted.isCompleted).isFalse()

        releaseInsert.complete(Unit)
        withTimeout(1_000.milliseconds) { sync.await() }
        withTimeout(1_000.milliseconds) { createReachedLock.await() }
        assertThat(mergeStarted.isCompleted).isTrue()
        withTimeout(1_000.milliseconds) { create.await() }
        Unit
    }

    // endregion

    // region findOrCreateByMembers

    @Test
    fun `returns existing pending channel found by members and refreshes the cache`() = runTest {
        val pendingChannel = pendingChannel()
        val data = createChannelData()
        whenever(findExistingChannelByMembersUseCase(data, CURRENT_USER_ID)).thenReturn(pendingChannel)

        val response = coordinator.findOrCreateByMembers(data, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(pendingChannel)
        verifyBlocking(channelsCache) { upsertPendingChannel(pendingChannel) }
        verifyBlocking(createPendingChannelUseCase, never()) { invoke(any(), any()) }
    }

    @Test
    fun `does not cache a real channel found by members`() = runTest {
        val realChannel = realChannel()
        val data = createChannelData()
        whenever(findExistingChannelByMembersUseCase(data, CURRENT_USER_ID)).thenReturn(realChannel)

        val response = coordinator.findOrCreateByMembers(data, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(realChannel)
        verifyBlocking(channelsCache, never()) { upsertPendingChannel(any()) }
    }

    @Test
    fun `creates a pending channel when no channel matches the members`() = runTest {
        val pendingChannel = pendingChannel()
        val data = createChannelData()
        whenever(findExistingChannelByMembersUseCase(data, CURRENT_USER_ID)).thenReturn(null)
        whenever(createPendingChannelUseCase(data, CURRENT_USER_ID))
            .thenReturn(SceytResponse.Success(pendingChannel))

        val response = coordinator.findOrCreateByMembers(data, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(pendingChannel)
    }

    // endregion

    // region findOrCreateByUri

    @Test
    fun `rejects a blank uri without touching the network`() = runTest {
        val response = coordinator.findOrCreateByUri(createChannelData(uri = "  "), CURRENT_USER_ID)

        assertThat(response).isInstanceOf(SceytResponse.Error::class.java)
        verifyBlocking(channelsRepository, never()) { getChannelByUri(any()) }
    }

    @Test
    fun `returns the locally known channel by uri without a remote lookup`() = runTest {
        val pendingChannel = pendingChannel()
        whenever(channelDao.getChannelByUri(CHANNEL_URI)).thenReturn(pendingChannel.toChannelDb())

        val response = coordinator.findOrCreateByUri(createChannelData(), CURRENT_USER_ID)

        assertThat(response.data?.id).isEqualTo(PENDING_CHANNEL_ID)
        verifyBlocking(channelsCache) { upsertPendingChannel(any()) }
        verifyBlocking(channelsRepository, never()) { getChannelByUri(any()) }
    }

    @Test
    fun `saves the remote channel found by uri when it is unknown locally`() = runTest {
        val realChannel = realChannel()
        whenever(channelDao.getChannelByUri(CHANNEL_URI)).thenReturn(null)
        whenever(channelsRepository.getChannelByUri(CHANNEL_URI))
            .thenReturn(SceytResponse.Success(realChannel))
        whenever(mergePendingDirectChannelsUseCase(realChannel, CURRENT_USER_ID)).thenReturn(realChannel)
        whenever(channelsCache.getCachedData()).thenReturn(hashMapOf())

        val response = coordinator.findOrCreateByUri(createChannelData(), CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(realChannel)
        verifyBlocking(insertChannelWithMembersUseCase) { invoke(eq(realChannel), any()) }
    }

    @Test
    fun `creates a pending channel when the uri is unknown locally and remotely`() = runTest {
        val pendingChannel = pendingChannel()
        val data = createChannelData()
        whenever(channelDao.getChannelByUri(CHANNEL_URI)).thenReturn(null)
        whenever(channelsRepository.getChannelByUri(CHANNEL_URI)).thenReturn(SceytResponse.Success(null))
        whenever(createPendingChannelUseCase(data, CURRENT_USER_ID))
            .thenReturn(SceytResponse.Success(pendingChannel))

        val response = coordinator.findOrCreateByUri(data, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(pendingChannel)
        verifyBlocking(insertChannelWithMembersUseCase, never()) { invoke(any(), any()) }
    }

    @Test
    fun `prefers a channel that appeared locally while the uri lookup was in flight`() = runTest {
        val pendingChannel = pendingChannel()
        val data = createChannelData()
        whenever(channelDao.getChannelByUri(CHANNEL_URI)).thenReturn(null, pendingChannel.toChannelDb())
        whenever(channelsRepository.getChannelByUri(CHANNEL_URI))
            .thenReturn(SceytResponse.Error(SceytException(1, "offline")))

        val response = coordinator.findOrCreateByUri(data, CURRENT_USER_ID)

        assertThat(response.data?.id).isEqualTo(PENDING_CHANNEL_ID)
        verifyBlocking(createPendingChannelUseCase, never()) { invoke(any(), any()) }
    }

    // endregion

    // region getRealByUriAndReconcile

    @Test
    fun `returns the remote error unchanged and writes nothing`() = runTest {
        val error = SceytResponse.Error<SceytChannel?>(SceytException(7, "offline"))
        whenever(channelsRepository.getChannelByUri(CHANNEL_URI)).thenReturn(error)

        val response = coordinator.getRealByUriAndReconcile(CHANNEL_URI, CURRENT_USER_ID)

        assertThat(response).isSameInstanceAs(error)
        verifyBlocking(insertChannelWithMembersUseCase, never()) { invoke(any(), any()) }
    }

    @Test
    fun `returns an empty remote result unchanged and writes nothing`() = runTest {
        val empty = SceytResponse.Success<SceytChannel?>(null)
        whenever(channelsRepository.getChannelByUri(CHANNEL_URI)).thenReturn(empty)

        val response = coordinator.getRealByUriAndReconcile(CHANNEL_URI, CURRENT_USER_ID)

        assertThat(response).isSameInstanceAs(empty)
        verifyBlocking(insertChannelWithMembersUseCase, never()) { invoke(any(), any()) }
    }

    @Test
    fun `frees the uri of the pending channel before migrating it onto the real one`() = runTest {
        val pendingChannel = pendingChannel()
        val realChannel = realChannel()
        val mergedChannel = realChannel.copy(subject = "migrated")
        whenever(channelsRepository.getChannelByUri(CHANNEL_URI))
            .thenReturn(SceytResponse.Success(realChannel))
        whenever(channelDao.getChannelByUri(CHANNEL_URI)).thenReturn(pendingChannel.toChannelDb())
        whenever(migratePendingChannelToRealChannelUseCase(any(), eq(realChannel))).thenReturn(mergedChannel)
        whenever(channelsCache.getCachedData()).thenReturn(hashMapOf())

        val response = coordinator.getRealByUriAndReconcile(CHANNEL_URI, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(mergedChannel)
        verifyBlocking(channelDao) { updateUri(PENDING_CHANNEL_ID, null) }
        verifyBlocking(insertChannelWithMembersUseCase) { invoke(eq(realChannel), any()) }
        verifyBlocking(mergePendingDirectChannelsUseCase, never()) { invoke(any<SceytChannel>(), any()) }
    }

    @Test
    fun `merges pending direct channels when no pending channel holds the uri`() = runTest {
        val realChannel = realChannel()
        val mergedChannel = realChannel.copy(subject = "direct merged")
        whenever(channelsRepository.getChannelByUri(CHANNEL_URI))
            .thenReturn(SceytResponse.Success(realChannel))
        whenever(channelDao.getChannelByUri(CHANNEL_URI)).thenReturn(null)
        whenever(mergePendingDirectChannelsUseCase(realChannel, CURRENT_USER_ID)).thenReturn(mergedChannel)
        whenever(channelsCache.getCachedData()).thenReturn(hashMapOf())

        val response = coordinator.getRealByUriAndReconcile(CHANNEL_URI, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(mergedChannel)
        verifyBlocking(channelDao, never()) { updateUri(any(), any()) }
        verifyBlocking(migratePendingChannelToRealChannelUseCase, never()) { invoke(any(), any()) }
    }

    @Test
    fun `keeps the uri when the local row by uri is the same real channel`() = runTest {
        val realChannel = realChannel()
        whenever(channelsRepository.getChannelByUri(CHANNEL_URI))
            .thenReturn(SceytResponse.Success(realChannel))
        whenever(channelDao.getChannelByUri(CHANNEL_URI)).thenReturn(realChannel.toChannelDb())
        whenever(mergePendingDirectChannelsUseCase(realChannel, CURRENT_USER_ID)).thenReturn(realChannel)
        whenever(channelsCache.getCachedData()).thenReturn(hashMapOf())

        val response = coordinator.getRealByUriAndReconcile(CHANNEL_URI, CURRENT_USER_ID)

        assertThat(response.data).isEqualTo(realChannel)
        verifyBlocking(channelDao, never()) { updateUri(any(), any()) }
    }

    @Test
    fun `notifies the cache that the pending channel with this uri became real`() = runTest {
        val pendingChannel = pendingChannel()
        val realChannel = realChannel()
        val config = mock<ChannelListConfig>()
        whenever(channelsRepository.getChannelByUri(CHANNEL_URI))
            .thenReturn(SceytResponse.Success(realChannel))
        whenever(channelDao.getChannelByUri(CHANNEL_URI)).thenReturn(null)
        whenever(mergePendingDirectChannelsUseCase(realChannel, CURRENT_USER_ID)).thenReturn(realChannel)
        whenever(channelsCache.getCachedData())
            .thenReturn(hashMapOf(config to hashMapOf(PENDING_CHANNEL_ID to pendingChannel)))

        coordinator.getRealByUriAndReconcile(CHANNEL_URI, CURRENT_USER_ID)

        verifyBlocking(channelsCache) { pendingChannelCreated(PENDING_CHANNEL_ID, realChannel) }
    }

    @Test
    fun `does not notify the cache for a non pending channel with the same uri`() = runTest {
        val realChannel = realChannel()
        val config = mock<ChannelListConfig>()
        whenever(channelsRepository.getChannelByUri(CHANNEL_URI))
            .thenReturn(SceytResponse.Success(realChannel))
        whenever(channelDao.getChannelByUri(CHANNEL_URI)).thenReturn(null)
        whenever(mergePendingDirectChannelsUseCase(realChannel, CURRENT_USER_ID)).thenReturn(realChannel)
        whenever(channelsCache.getCachedData())
            .thenReturn(hashMapOf(config to hashMapOf(REAL_CHANNEL_ID to realChannel)))

        coordinator.getRealByUriAndReconcile(CHANNEL_URI, CURRENT_USER_ID)

        verifyBlocking(channelsCache, never()) { pendingChannelCreated(any(), any()) }
    }

    // endregion

    private fun createChannelData(uri: String = CHANNEL_URI) = CreateChannelData(
        type = "direct",
        uri = uri,
    )

    private fun pendingChannel() = createChannel(
        id = PENDING_CHANNEL_ID,
        pinnedAt = 0,
        createdAt = 1,
    ).copy(
        uri = CHANNEL_URI,
        pending = true,
    )

    private fun realChannel() = createChannel(
        id = REAL_CHANNEL_ID,
        pinnedAt = 0,
        createdAt = 2,
    ).copy(uri = CHANNEL_URI)

    private fun SceytChannel.toChannelDb() = ChannelDb(
        channelEntity = toChannelEntity(),
        members = null,
        lastMessage = null,
        createdBy = null,
        newReactions = null,
        draftMessage = null,
        pendingReactions = null,
    )
}
