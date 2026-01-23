package com.sceyt.chatuikit.persistence.file_transfer

/**
 * Validates transfer state transitions to prevent invalid state changes.
 * 
 * Rules:
 * - Once a transfer is completed (Downloaded/Uploaded), only ThumbLoaded and FilePathChanged are allowed
 * - Progress cannot go backwards during Downloading/Uploading states
 */
object TransferStateValidator {

    /**
     * Validates if a transfer state change is allowed.
     * 
     * @param currentState The current transfer state
     * @param newState The new state to transition to
     * @param currentProgress The current progress percentage
     * @param newProgress The new progress percentage
     * @return true if the transition is valid, false otherwise
     */
    fun isValidStateTransition(
        currentState: TransferState?,
        newState: TransferState,
        currentProgress: Float,
        newProgress: Float
    ): Boolean {
        // If already completed (Downloaded/Uploaded), only allow ThumbLoaded and FilePathChanged
        if (currentState?.isCompleted() == true) {
            return newState == TransferState.ThumbLoaded || newState == TransferState.FilePathChanged
        }

        // For progress updates (Downloading/Uploading), don't allow backward progress
        if (newState == TransferState.Downloading || newState == TransferState.Uploading) {
            if (newProgress < currentProgress) {
                return false
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
