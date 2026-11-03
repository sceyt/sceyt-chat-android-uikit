package com.sceyt.chatuikit.persistence.file_transfer

/**
 * Validates transfer state transitions to prevent invalid state changes.
 * 
 * Rules:
 * - Once a transfer is completed (Downloaded/Uploaded), only completed states, ThumbLoaded,
 *   and FilePathChanged are allowed
 * - Progress belongs to an attempt and may restart from zero
 */
object TransferStateValidator {

    /**
     * Validates if a transfer state change is allowed.
     * Progress parameters remain for existing callers; coordinators validate attempt ownership.
     * 
     * @param currentState The current transfer state
     * @param newState The new state to transition to
     * @param currentProgress The current progress percentage
     * @param newProgress The new progress percentage
     * @return true if the transition is valid, false otherwise
     */
    @Suppress("UNUSED_PARAMETER")
    fun isValidStateTransition(
        currentState: TransferState?,
        newState: TransferState,
        currentProgress: Float,
        newProgress: Float
    ): Boolean {
        // Uploaded/Downloaded can describe the same finished attachment through upload/download flows.
        if (currentState?.isCompleted() == true) {
            return when (newState) {
                TransferState.ThumbLoaded,
                TransferState.FilePathChanged -> true

                else -> newState.isCompleted()
            }
        }

        return true
    }

    /**
     * Validates if a progress update is allowed.
     * This is a convenience method for progress-only updates.
     * 
     * @param currentState The current transfer state
     * @param newState The new state (should be Downloading or Uploading)
     * @param currentProgress The current progress percentage
     * @param newProgress The new progress percentage
     * @return true if the progress update is valid, false otherwise
     */
    fun isValidProgressUpdate(
        currentState: TransferState?,
        newState: TransferState,
        currentProgress: Float,
        newProgress: Float
    ): Boolean = isValidStateTransition(currentState, newState, currentProgress, newProgress)
}
