package com.sceyt.chatuikit.persistence.file_transfer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransferStateValidatorTest {

    @Test
    fun `completed states can transition between upload and download representations`() {
        assertThat(
            TransferStateValidator.isValidStateTransition(
                currentState = TransferState.Uploaded,
                newState = TransferState.Downloaded,
                currentProgress = 100f,
                newProgress = 100f
            )
        ).isTrue()

        assertThat(
            TransferStateValidator.isValidStateTransition(
                currentState = TransferState.Downloaded,
                newState = TransferState.Uploaded,
                currentProgress = 100f,
                newProgress = 100f
            )
        ).isTrue()
    }

    @Test
    fun `completed state can still accept non-transfer updates`() {
        assertThat(
            TransferStateValidator.isValidStateTransition(
                currentState = TransferState.Uploaded,
                newState = TransferState.ThumbLoaded,
                currentProgress = 100f,
                newProgress = 100f
            )
        ).isTrue()

        assertThat(
            TransferStateValidator.isValidStateTransition(
                currentState = TransferState.Uploaded,
                newState = TransferState.FilePathChanged,
                currentProgress = 100f,
                newProgress = 100f
            )
        ).isTrue()
    }

    @Test
    fun `completed state cannot regress to running state`() {
        assertThat(
            TransferStateValidator.isValidStateTransition(
                currentState = TransferState.Uploaded,
                newState = TransferState.Downloading,
                currentProgress = 100f,
                newProgress = 50f
            )
        ).isFalse()
    }
}
