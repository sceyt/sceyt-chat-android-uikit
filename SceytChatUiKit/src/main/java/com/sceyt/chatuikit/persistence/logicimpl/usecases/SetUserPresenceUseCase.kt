package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.onError
import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal class SetUserPresenceUseCase {

    private companion object {
        private const val TAG = "SetUserPresenceUseCase"
        private const val MAX_RETRY_COUNT = 5
        private const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        setUserPresenceWithRetry()
    }

    private suspend fun setUserPresenceWithRetry(
        retryCount: Int = 0,
        delayMs: Long = INITIAL_RETRY_DELAY_MS,
    ) {
        val state = SceytChatUIKit.config.presenceConfig.defaultPresenceState
        SceytChatUIKit.chatUIFacade.userInteractor.setPresenceState(state).onError { exception ->
            if (retryCount < MAX_RETRY_COUNT && ConnectionEventManager.isConnected) {
                SceytLog.i(
                    TAG,
                    "setUserPresence state:$state failed, retrying (${retryCount + 1}/$MAX_RETRY_COUNT): ${exception?.message}"
                )
                // Exponential backoff
                delay(delayMs)
                setUserPresenceWithRetry(retryCount + 1, delayMs * 2)
            } else {
                SceytLog.e(
                    TAG,
                    "setUserPresence state:$state failed after $MAX_RETRY_COUNT retries: ${exception?.message}"
                )
            }
        }
    }
}