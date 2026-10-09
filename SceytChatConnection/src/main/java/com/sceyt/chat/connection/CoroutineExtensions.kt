package com.sceyt.chat.connection

import kotlinx.coroutines.CancellationException

/** Returns ordinary exceptions as failures while allowing coroutine cancellation to propagate. */
internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
