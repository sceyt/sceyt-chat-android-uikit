package com.sceyt.chatuikit.data

import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SDKErrorTypeEnum
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.delay

private const val TAG = "SceytResponseRetry"

/** Error code for request timeout when type field is empty */
private const val ERROR_CODE_REQUEST_TIMEOUT = 9902

/**
 * Retries the given [block] if it returns a [SceytResponse.Error] with a resendable error.
 *
 * @param maxRetries Maximum number of retry attempts (default: 3)
 * @param initialDelayMs Initial delay in milliseconds before the first retry (default: 1000ms)
 * @param block The suspend function to execute and potentially retry
 * @return The result of the [block] execution, either success or the last error after all retries
 */
suspend fun <T> retryOnResendableError(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    block: suspend () -> SceytResponse<T>
): SceytResponse<T> {
    var currentDelay = initialDelayMs
    var lastResponse: SceytResponse<T> = block()
    var retryAttempted = false

    repeat(maxRetries) { attempt ->
        when (lastResponse) {
            is SceytResponse.Success -> {
                if (retryAttempted) {
                    SceytLog.i(TAG, "Request succeeded after $attempt retry attempt(s)")
                }
                return lastResponse
            }
            is SceytResponse.Error -> {
                val exception = lastResponse.exception
                
                if (!isResendableError(exception)) {
                    // Non-resendable error, return immediately
                    return lastResponse
                }

                retryAttempted = true
                
                // Log retry attempt
                SceytLog.i(
                    TAG,
                    "Resendable error (code: ${exception?.code}, type: ${exception?.type}), " +
                            "retrying... attempt ${attempt + 1}/$maxRetries, delay: ${currentDelay}ms"
                )

                // Wait before retry with exponential backoff
                delay(currentDelay)
                currentDelay *= 2

                // Retry the operation
                lastResponse = block()
            }
        }
    }

    return lastResponse
}

/**
 * Checks if the given [SceytException] represents a resendable error.
 * 
 * An error is considered resendable if:
 * 1. The error type is marked as resendable in [SDKErrorTypeEnum], OR
 * 2. The error code matches a known resendable error code (e.g., 9902 for request timeout)
 */
private fun isResendableError(exception: SceytException?): Boolean {
    if (exception == null) return false
    
    // Check by error type first
    val errorType = SDKErrorTypeEnum.fromValue(exception.type)
    if (errorType?.isResendable == true) {
        return true
    }
    
    // Check by error code for cases where type is empty
    return when (exception.code) {
        ERROR_CODE_REQUEST_TIMEOUT -> true
        else -> false
    }
}
