package com.sceyt.chat.demo.presentation.welcome.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.root.PageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class WelcomeViewModel(
    private val preference: AppSharedPreference,
    private val connectionProvider: SceytConnectionProvider
) : BaseViewModel() {

    private val _logInLiveData = MutableLiveData<Boolean>()
    val logInLiveData: LiveData<Boolean> = _logInLiveData

    fun loginUser(
        userId: String,
    ) {
        viewModelScope.launch {
            pageStateLiveDataInternal.value = PageState.StateLoading()

            val result = connectUser(userId)
            if (result.isSuccess) {
                preference.setString(AppSharedPreference.PREF_USER_ID, userId)
                pageStateLiveDataInternal.value = PageState.Nothing
            } else
                pageStateLiveDataInternal.value = PageState.StateError(
                    null, result.exceptionOrNull()?.message
                )

            _logInLiveData.value = result.isSuccess
        }
    }

    private suspend fun connectUser(
        userId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {

        val getTokenResult = suspendCancellableCoroutine { continuation ->
            connectionProvider.connectChatClient(userId) { isStarted, exception ->
                if (!isStarted) {
                    pageStateLiveDataInternal.postValue(
                        PageState.StateError(null, exception?.message)
                    )
                    continuation.safeResume(
                        Result.failure(
                            exception ?: Exception("Connection failed")
                        )
                    )
                } else continuation.safeResume(Result.success(true))
            }
        }

        if (getTokenResult.isFailure) {
            return@withContext getTokenResult
        }

        return@withContext ConnectionEventManager.awaitToConnectSceytWithResult(8.seconds.inWholeMilliseconds)
    }
}