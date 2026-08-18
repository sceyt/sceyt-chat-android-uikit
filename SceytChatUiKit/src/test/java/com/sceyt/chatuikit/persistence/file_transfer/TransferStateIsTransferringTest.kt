package com.sceyt.chatuikit.persistence.file_transfer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransferStateIsTransferringTest {

    @Test
    fun `states with an unfinished transfer are transferring`() {
        val transferring = listOf(
            TransferState.PendingUpload,
            TransferState.PendingDownload,
            TransferState.Uploading,
            TransferState.Downloading,
            TransferState.Preparing,
            TransferState.WaitingToUpload,
            TransferState.FilePathChanged,
        )

        transferring.forEach {
            assertThat(it.isTransferring()).isTrue()
        }
    }

    @Test
    fun `finished, paused and failed states are not transferring`() {
        val notTransferring = listOf(
            TransferState.Uploaded,
            TransferState.Downloaded,
            TransferState.PauseUpload,
            TransferState.PauseDownload,
            TransferState.ErrorUpload,
            TransferState.ErrorDownload,
            TransferState.ThumbLoaded,
        )

        notTransferring.forEach {
            assertThat(it.isTransferring()).isFalse()
        }
    }

    @Test
    fun `unknown state is not transferring`() {
        assertThat(null.isTransferring()).isFalse()
    }
}