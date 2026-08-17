package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.persistence.logicimpl.transferTask
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
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

    @Test
    fun `shared upload registers its task at the service boundary`() {
        val attachment = attachment()
        val task = transferTask(attachment)

        service.uploadSharedFile(attachment, task)

        assertThat(service.findTransferTask(attachment)).isSameInstanceAs(task)
        verify(logic).uploadSharedFile(attachment, task)
    }

    @Test
    fun `new upload attempt replaces the previous worker task`() {
        val attachment = attachment()
        val previousTask = transferTask(attachment)
        val currentTask = transferTask(attachment)

        service.upload(attachment, previousTask)
        service.upload(attachment, currentTask)

        assertThat(service.findTransferTask(attachment)).isSameInstanceAs(currentTask)
        verify(logic).uploadFile(attachment, previousTask)
        verify(logic).uploadFile(attachment, currentTask)
    }

    @Test
    fun `duplicate download keeps the task that owns the active operation`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val firstTask = transferTask(attachment)
        val duplicateTask = transferTask(attachment)

        service.download(attachment, firstTask)
        service.download(attachment, duplicateTask)

        assertThat(service.findTransferTask(attachment)).isSameInstanceAs(firstTask)
        verify(logic, times(2)).downloadFile(attachment, firstTask)
        verify(logic, never()).downloadFile(attachment, duplicateTask)
    }

    @Test
    fun `cancel all cancels default and legacy transfers`() {
        service.setCustomListener(legacyListeners)

        service.cancelAllTransfers()

        verify(logic).cancelAll()
        verify(legacyListeners).cancelAllTransfers()
    }
}
