package com.sceyt.chatuikit.persistence.di

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FileTransferServiceKoinScopeTest {
    private val appScope = CoroutineScope(SupervisorJob())

    @Before
    fun setUp() {
        stopKoin()
        val context: Context = RuntimeEnvironment.getApplication()
        SceytKoinApp.koinApp = startKoin {
            modules(
                appModules,
                logicModule,
                coroutineModule,
                module {
                    single<Context> { context }
                    single<PersistenceAttachmentLogic> { mock() }
                    single<CoroutineScope> { appScope }
                },
            )
        }
    }

    @After
    fun tearDown() {
        appScope.cancel()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `cancelling all file transfers keeps app scope work running`() {
        val appWork = appScope.launch { awaitCancellation() }
        val service = checkNotNull(SceytKoinApp.koinApp).koin.get<FileTransferService>()

        service.cancelAllTransfers()

        assertThat(appWork.isActive).isTrue()
    }
}
