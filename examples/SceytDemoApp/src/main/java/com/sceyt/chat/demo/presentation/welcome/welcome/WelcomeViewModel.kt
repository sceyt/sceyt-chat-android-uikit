package com.sceyt.chat.demo.presentation.welcome.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.root.PageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

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
            } else
                pageStateLiveDataInternal.value = PageState.StateError(
                    null, result.exceptionOrNull()?.message
                )

            _logInLiveData.value = result.isSuccess
        }
    }

    private suspend fun connectUser(userId: String) = withContext(Dispatchers.IO) {
        var job: Job? = null
        val data = suspendCancellableCoroutine<Result<Boolean>> { continuation ->
            job = ConnectionEventManager.onChangedConnectStatusFlow.onEach {
                when (it.state) {
                    ConnectionState.Connected -> {
                        continuation.resume(Result.success(true))
                        job?.cancel()
                    }

                    ConnectionState.Disconnected, ConnectionState.Failed -> {
                        continuation.resume(
                            Result.failure(Exception(it.exception?.message))
                        )
                        job?.cancel()
                    }

                    else -> Unit
                }
            }.launchIn(this)

            connectionProvider.connectChatClient(userId) { isStarted, exception ->
                if (!isStarted) {
                    continuation.resume(Result.failure(Throwable(exception?.message)))
                    pageStateLiveDataInternal.postValue(PageState.StateError(
                        null, exception?.message
                    ))
                    job?.cancel()
                }
            }
        }

        return@withContext data
    }
}