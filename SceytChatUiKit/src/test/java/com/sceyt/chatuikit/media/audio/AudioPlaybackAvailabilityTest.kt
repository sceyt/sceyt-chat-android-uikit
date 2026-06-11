package com.sceyt.chatuikit.media.audio

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AudioPlaybackAvailabilityTest {

    @Test
    fun completedTransferWithLocalPath_isAvailable() {
        assertThat(isAudioPlaybackAvailable(FILE_PATH, TransferState.Downloaded)).isTrue()
        assertThat(isAudioPlaybackAvailable(FILE_PATH, TransferState.Uploaded)).isTrue()
    }

    @Test
    fun activeOrPendingTransfer_isNotAvailable() {
        assertThat(isAudioPlaybackAvailable(FILE_PATH, TransferState.PendingDownload)).isFalse()
        assertThat(isAudioPlaybackAvailable(FILE_PATH, TransferState.Downloading)).isFalse()
        assertThat(isAudioPlaybackAvailable(FILE_PATH, TransferState.Uploading)).isFalse()
        assertThat(isAudioPlaybackAvailable(FILE_PATH, TransferState.PauseDownload)).isFalse()
    }

    @Test
    fun completedTransferWithoutLocalPath_isNotAvailable() {
        assertThat(isAudioPlaybackAvailable(null, TransferState.Downloaded)).isFalse()
        assertThat(isAudioPlaybackAvailable("", TransferState.Uploaded)).isFalse()
    }

    @Test
    fun currentTransferData_overridesStaleCompletedAttachmentState() {
        val item = mock<AttachmentDataProvider>()
        val attachment = mock<SceytAttachment>()
        whenever(item.filePath).thenReturn(FILE_PATH)
        whenever(item.attachment).thenReturn(attachment)
        whenever(attachment.transferState).thenReturn(TransferState.Downloaded)
        whenever(item.transferData).thenReturn(
            TransferData(
                messageTid = 1L,
                progressPercent = 50f,
                state = TransferState.Downloading,
                filePath = FILE_PATH,
                url = null
            )
        )

        assertThat(item.isAudioPlaybackAvailable()).isFalse()
    }

    private companion object {
        const val FILE_PATH = "/voice/message.aac"
    }
}
