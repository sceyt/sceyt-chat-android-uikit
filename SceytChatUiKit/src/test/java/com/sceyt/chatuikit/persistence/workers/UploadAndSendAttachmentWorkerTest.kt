package com.sceyt.chatuikit.persistence.workers

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import org.junit.Test

class UploadAndSendAttachmentWorkerTest {
    @Test
    fun `unrelated worker run does not restart paused upload`() {
        assertThat(
            shouldStartUpload(
                state = TransferState.PauseUpload,
                resumePausedUpload = false,
            ),
        ).isFalse()
    }

    @Test
    fun `explicit worker resume restarts paused upload`() {
        assertThat(
            shouldStartUpload(
                state = TransferState.PauseUpload,
                resumePausedUpload = true,
            ),
        ).isTrue()
    }

    @Test
    fun `normal upload does not require explicit resume`() {
        assertThat(
            shouldStartUpload(
                state = TransferState.PendingUpload,
                resumePausedUpload = false,
            ),
        ).isTrue()
    }
}
