package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
class FileTransferServiceImplTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val logic = mock<FileTransferLogic>()
    private val service = FileTransferServiceImpl(context, logic)

    @Before
    fun setUp() {
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<FileTransferService> { service }
                single<PersistenceAttachmentLogic> { mock() }
                single<FileChecksumDao> { mock() }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) {
                    Dispatchers.Unconfined
                }
            })
        }
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `task is configured before upload starts`() {
        val attachment = attachment()
        var configuredTask: TransferTask? = null
        doAnswer {
            assertThat(configuredTask).isSameInstanceAs(it.arguments[1])
        }.`when`(logic).uploadFile(org.mockito.kotlin.eq(attachment), org.mockito.kotlin.any())

        val task = service.upload(attachment) {
            configuredTask = this
        }

        assertThat(task).isSameInstanceAs(configuredTask)
        verify(logic).uploadFile(attachment, task)
    }

    @Test
    fun `duplicate download reuses its active task`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val updatedAttachment = attachment.copy(url = "updated-url")

        val firstTask = service.download(attachment)
        val duplicateTask = service.download(updatedAttachment)

        assertThat(duplicateTask).isSameInstanceAs(firstTask)
        verify(logic, org.mockito.kotlin.times(2)).downloadFile(attachment, firstTask)
    }

    @Test
    fun `download notifies completion listener`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        var result: Result<SceytAttachment>? = null
        doAnswer {
            (it.arguments[1] as TransferTask).downloadCallback
                ?.onResult(SceytResponse.Success("downloaded-file"))
            Unit
        }.`when`(logic).downloadFile(org.mockito.kotlin.eq(attachment), org.mockito.kotlin.any())

        service.download(attachment) {
            addOnCompletionListener("test") { result = it }
        }

        assertThat(result?.getOrNull()?.filePath).isEqualTo("downloaded-file")
    }

    @Test
    fun `upload creates a new task after terminal cleanup`() {
        val attachment = attachment()
        val completedTask = service.upload(attachment)

        service.removeTransferTask(attachment.messageTid)
        val retryTask = service.upload(attachment)

        assertThat(retryTask).isNotSameInstanceAs(completedTask)
    }

    @Test
    fun `cancel all clears tasks and coordinator state`() {
        val attachment = attachment()
        service.upload(attachment)

        service.cancelAllTransfers()

        assertThat(service.getTasks()).isEmpty()
        verify(logic).cancelAll()
    }
}
