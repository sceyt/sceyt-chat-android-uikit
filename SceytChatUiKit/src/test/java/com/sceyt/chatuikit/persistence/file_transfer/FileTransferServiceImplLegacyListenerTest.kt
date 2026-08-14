package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.persistence.logicimpl.transferTask
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FileTransferServiceImplLegacyListenerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val logic = mock<FileTransferLogic>()
    private val legacyListeners = mock<FileTransferListeners.Listeners>()
    private val service = FileTransferServiceImpl(context, logic)

    @Test
    fun `legacy listener takes precedence over default upload logic`() {
        val attachment = attachment()
        val task = transferTask(attachment)
        service.setCustomListener(legacyListeners)

        service.upload(attachment, task)

        verify(legacyListeners).upload(attachment, task)
        verify(logic, never()).uploadFile(attachment, task)
    }

    @Test
    fun `legacy listener takes precedence over default download logic`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        service.setCustomListener(legacyListeners)

        service.download(attachment, task)

        verify(legacyListeners).download(attachment, task)
        verify(logic, never()).downloadFile(attachment, task)
    }
}
