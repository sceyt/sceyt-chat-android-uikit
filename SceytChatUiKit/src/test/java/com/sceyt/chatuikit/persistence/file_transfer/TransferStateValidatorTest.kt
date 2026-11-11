package com.sceyt.chatuikit.persistence.file_transfer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransferStateValidatorTest {

    @Test
    fun `progress updates allow upload and download without a previous state`() {
        listOf(TransferState.Uploading, TransferState.Downloading).forEach { state ->
            assertThat(
                TransferStateValidator.isValidProgressUpdate(
                    currentState = null,
                    newState = state,
                    currentProgress = 0f,
                    newProgress = 0f,
                )
            ).isTrue()
        }
    }

    @Test
    fun `progress updates allow advancing unchanged and restarted percentages`() {
        listOf(TransferState.Uploading, TransferState.Downloading).forEach { state ->
            listOf(25f to 50f, 25f to 25f, 75f to 0f, 75f to 10f).forEach { (previous, next) ->
                assertThat(
                    TransferStateValidator.isValidProgressUpdate(
                        currentState = state,
                        newState = state,
                        currentProgress = previous,
                        newProgress = next,
                    )
                ).isTrue()
            }
        }
    }

    @Test
    fun `progress updates cannot restart a completed upload or download`() {
        listOf(TransferState.Uploaded, TransferState.Downloaded).forEach { completed ->
            listOf(TransferState.Uploading, TransferState.Downloading).forEach { running ->
                assertThat(
                    TransferStateValidator.isValidProgressUpdate(
                        currentState = completed,
                        newState = running,
                        currentProgress = 100f,
                        newProgress = 0f,
                    )
                ).isFalse()
            }
        }
    }

    @Test
    fun `upload and download attempts can restart below previous progress`() {
        listOf(
            TransferState.PauseUpload to TransferState.Uploading,
            TransferState.PauseDownload to TransferState.Downloading,
            TransferState.ErrorUpload to TransferState.Uploading,
            TransferState.ErrorDownload to TransferState.Downloading,
            TransferState.Uploading to TransferState.Uploading,
            TransferState.Downloading to TransferState.Downloading,
        ).forEach { (previous, next) ->
            assertThat(
                TransferStateValidator.isValidStateTransition(
                    currentState = previous,
                    newState = next,
                    currentProgress = 75f,
                    newProgress = 0f,
                )
            ).isTrue()
        }
    }

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
