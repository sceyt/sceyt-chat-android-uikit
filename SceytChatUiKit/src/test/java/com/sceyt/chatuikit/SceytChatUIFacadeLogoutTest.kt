package com.sceyt.chatuikit

import androidx.work.Operation
import androidx.work.impl.WorkManagerImpl
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.SettableFuture
import com.sceyt.chat.ChatClient
import com.sceyt.chatuikit.data.constants.SceytConstants.SCEYT_WORKER_TAG
import com.sceyt.chatuikit.notifications.SceytNotifications
import com.sceyt.chatuikit.notifications.push.PushNotificationHandler
import com.sceyt.chatuikit.persistence.database.cleaner.DatabaseCleaner
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.push.delegates.FirebaseMessagingDelegate
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["com.sceyt.chatuikit"])
@OptIn(ExperimentalCoroutinesApi::class)
class SceytChatUIFacadeLogoutTest {
    private val workManager = mock<WorkManagerImpl>()
    private val cancellation = SettableFuture.create<Operation.State.SUCCESS>()
    private val service = mock<FileTransferService>()
    private val cleaner = mock<DatabaseCleaner>()
    private val preferences = mock<SceytSharedPreference>()
    private val cache = mock<ChannelsCache>()
    private val syncManager = mock<SceytSyncManager>()
    private val client = mock<ChatClient>()
    private val notifications = mock<PushNotificationHandler>()
    private val unhandledErrors = mutableListOf<Throwable>()
    private val results = mutableListOf<Result<Boolean>>()
    private lateinit var dispatcherMock: MockedStatic<Dispatchers>
    private lateinit var clientMock: MockedStatic<ChatClient>
    private lateinit var pushMock: MockedStatic<FirebaseMessagingDelegate>
    private lateinit var facade: SceytChatUIFacade
    private lateinit var previousNotifications: SceytNotifications
    private var facadeJob: Job? = null
    private val previousExceptionHandler = Thread.currentThread().uncaughtExceptionHandler

    @Before
    fun setUp() {
        // Keep static SDK mocks and coroutine callbacks on this test's thread.
        val dispatcher = Dispatchers.Unconfined
        dispatcherMock = Mockito.mockStatic(Dispatchers::class.java, Mockito.CALLS_REAL_METHODS)
        dispatcherMock.`when`<CoroutineDispatcher> { Dispatchers.IO }.thenReturn(dispatcher)
        clientMock = Mockito.mockStatic(ChatClient::class.java)
        clientMock.`when`<ChatClient> { ChatClient.getClient() }.thenReturn(client)
        pushMock = Mockito.mockStatic(FirebaseMessagingDelegate::class.java)
        pushMock.`when`<Unit> { FirebaseMessagingDelegate.unregisterFirebaseToken(any()) }
            .thenAnswer {
                it.getArgument<(Result<Boolean>) -> Unit>(0)(Result.success(true))
                null
            }
        Thread.currentThread().uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, error ->
            unhandledErrors += error
        }
        WorkManagerImpl.setDelegate(workManager)
        val operation = mock<Operation>()
        whenever(operation.result).thenReturn(cancellation)
        whenever(workManager.cancelAllWorkByTag(SCEYT_WORKER_TAG)).thenReturn(operation)
        val context = RuntimeEnvironment.getApplication()
        // UIKit is not initialized here, so its lazy notifications may not exist yet.
        previousNotifications = runCatching { SceytChatUIKit.notifications }
            .getOrElse { SceytNotifications(context) }
        SceytChatUIKit.notifications = SceytNotifications(context).apply {
            pushNotification.notificationHandler = notifications
        }
        facade = SceytChatUIFacade(
            context = context,
            preferences = preferences,
            databaseCleaner = cleaner,
            channelsCache = cache,
            sceytSyncManager = syncManager,
            filesTransferService = service,
            channelInteractor = mock(),
            messageInteractor = mock(),
            channelMemberInteractor = mock(),
            userInteractor = mock(),
            attachmentInteractor = mock(),
            messageReactionInteractor = mock(),
            messageMarkerInteractor = mock(),
            channelInviteKeyInteractor = mock(),
        )
    }

    @After
    fun tearDown() {
        facadeJob?.cancel()
        WorkManagerImpl.setDelegate(null)
        pushMock.close()
        clientMock.close()
        dispatcherMock.close()
        SceytChatUIKit.notifications = previousNotifications
        Thread.currentThread().uncaughtExceptionHandler = previousExceptionHandler
    }

    @Test
    fun `failed worker cancellation does not stop logout cleanup`() = runBlocking {
        val job = startLogout()

        cancellation.setException(IllegalStateException("WorkManager cancellation failed"))
        withTimeout(1.seconds) { job.join() }

        verifyLogoutCleanup()
    }

    @Test
    fun `successful worker cancellation completes logout cleanup`() = runBlocking {
        val job = startLogout()

        cancellation.set(Operation.SUCCESS)
        withTimeout(1.seconds) { job.join() }

        verifyLogoutCleanup()
    }

    @Test
    fun `cancelled worker operation does not cancel logout`() = runBlocking {
        val job = startLogout()

        cancellation.cancel(false)
        withTimeout(1.seconds) { job.join() }

        assertThat(job.isCancelled).isFalse()
        verifyLogoutCleanup()
    }

    @Test
    fun `cancelling logout itself does not continue cleanup`() = runBlocking {
        val job = startLogout()

        job.cancel()
        withTimeout(1.seconds) { job.join() }

        verify(service, never()).cancelAllTransfers()
        verify(cleaner, never()).cleanDatabase()
        verify(client, never()).disconnect()
        assertThat(results).isEmpty()
        assertThat(unhandledErrors).isEmpty()
    }

    private fun startLogout(): Job {
        return facade.logOut { results += it }.also { facadeJob = it.parent }
    }

    private suspend fun verifyLogoutCleanup() {
        verify(syncManager).cancelSync()
        verify(service).cancelAllTransfers()
        verify(cleaner).cleanDatabase()
        verify(preferences).clear()
        verify(cache).clearAll()
        pushMock.verify { FirebaseMessagingDelegate.unregisterFirebaseToken(any()) }
        verify(client).disconnect()
        verify(notifications).cancelAllNotifications()
        assertThat(results.single().getOrNull()).isTrue()
        assertThat(unhandledErrors).isEmpty()
    }
}
