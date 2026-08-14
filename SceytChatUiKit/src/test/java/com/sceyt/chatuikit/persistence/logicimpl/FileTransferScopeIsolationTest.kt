package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FileTransferScopeIsolationTest {
    private lateinit var context: Context
    private lateinit var destinationFile: File
    private lateinit var service: TestFileTransferService
    private lateinit var transport: RecordingFileTransferTransport
    private lateinit var attachmentLogic: PersistenceAttachmentLogic
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var sharedScope: CoroutineScope

    @Before
    fun setUp() {
        stopKoin()
        context = RuntimeEnvironment.getApplication()
        destinationFile = File(context.cacheDir, "scope-isolation-tests/download.bin")
        destinationFile.parentFile?.mkdirs()
        destinationFile.delete()
        service = TestFileTransferService()
        transport = RecordingFileTransferTransport()
        attachmentLogic = mock()
        sharedScope = TestScope(UnconfinedTestDispatcher())
        previousFileTransfer = SceytChatUIKit.fileTransfer
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@FileTransferScopeIsolationTest.transport
            destinationProvider = FileTransferDestinationProvider { _, _ -> destinationFile }
        }
        SceytKoinApp.koinApp = startKoin {
            modules(module { single<FileTransferService> { service } })
        }
    }

    @After
    fun tearDown() {
        destinationFile.parentFile?.deleteRecursively()
        SceytChatUIKit.fileTransfer = previousFileTransfer
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `cancelling uploads leaves downloads on the shared scope running`() {
        val uploads = AttachmentUploadCoordinator(context, attachmentLogic, sharedScope)
        val downloads = AttachmentDownloadCoordinator(context, sharedScope)
        val uploaded = attachment(messageTid = 90L, filePath = "/tmp/upload.txt")
        val downloaded = attachment(messageTid = 91L, state = TransferState.PendingDownload)

        uploads.uploadFile(uploaded, transferTask(uploaded))
        downloads.downloadFile(downloaded, transferTask(downloaded))
        uploads.cancelAll()

        assertThat(transport.uploadCalls.single().cancelled).isTrue()
        assertThat(transport.downloadCalls.single().cancelled).isFalse()

        transport.downloadCalls.single().succeed(destinationFile.path)

        assertThat(transport.downloadCalls.single().cancelled).isFalse()
    }

    @Test
    fun `cancelling downloads leaves uploads on the shared scope running`() {
        val uploads = AttachmentUploadCoordinator(context, attachmentLogic, sharedScope)
        val downloads = AttachmentDownloadCoordinator(context, sharedScope)
        val uploaded = attachment(messageTid = 92L, filePath = "/tmp/upload.txt")
        val downloaded = attachment(messageTid = 93L, state = TransferState.PendingDownload)

        uploads.uploadFile(uploaded, transferTask(uploaded))
        downloads.downloadFile(downloaded, transferTask(downloaded))
        downloads.cancelAll()

        assertThat(transport.downloadCalls.single().cancelled).isTrue()
        assertThat(transport.uploadCalls.single().cancelled).isFalse()
    }

    @Test
    fun `logic cancelAll stops both uploads and downloads`() {
        val logic = FileTransferLogicImpl(
            context = context,
            attachmentLogic = attachmentLogic,
            thumbPathResolver = mock(),
            scope = sharedScope,
        )
        val uploaded = attachment(messageTid = 94L, filePath = "/tmp/upload.txt")
        val downloaded = attachment(messageTid = 95L, state = TransferState.PendingDownload)

        logic.uploadFile(uploaded, transferTask(uploaded))
        logic.downloadFile(downloaded, transferTask(downloaded))
        logic.cancelAll()

        assertThat(transport.uploadCalls.single().cancelled).isTrue()
        assertThat(transport.downloadCalls.single().cancelled).isTrue()
    }
}