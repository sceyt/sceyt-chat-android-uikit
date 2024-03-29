package com.sceyt.chat.demo.presentation.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.Constants
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.persistence.PersistenceUsersMiddleWare
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.root.PageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class LoginViewModel(private val preference: AppSharedPreference,
                     private val connectionProvider: SceytConnectionProvider) : BaseViewModel() {

    private val usersMiddleWare: PersistenceUsersMiddleWare by lazy { SceytKitClient.getUserMiddleWare() }
    private val _logInLiveData = MutableLiveData<Boolean>()
    val logInLiveData: LiveData<Boolean> = _logInLiveData

    fun loginUser(userId: String, displayName: String) {
        notifyPageLoadingState(false)
        viewModelScope.launch {
            val result = connectUser(userId)
            if (result.isSuccess) {
                preference.setUserId(userId)
                updateProfile(displayName)
            } else
                pageStateLiveDataInternal.value = PageState.StateError(null, result.exceptionOrNull()?.message
                        ?: "Connection failed")

            pageStateLiveDataInternal.value = PageState.StateLoading(false)
            _logInLiveData.value = result.isSuccess
        }
    }

    fun loginWithRandomUser() {
        notifyPageLoadingState(false)
        viewModelScope.launch {
            val randomUserId = Constants.users.random()
            val result = connectUser(randomUserId)
            if (result.isSuccess) {
                preference.setUserId(randomUserId)
            } else
                pageStateLiveDataInternal.value = PageState.StateError(null, result.exceptionOrNull()?.message
                        ?: "Connection failed")

            pageStateLiveDataInternal.value = PageState.StateLoading(false)
            _logInLiveData.value = result.isSuccess
        }
    }

    fun isLoggedIn() = preference.getUserId().isNullOrBlank().not()

    private suspend fun updateProfile(displayName: String) = withContext(Dispatchers.IO) {
        val currentUser: User? = ClientWrapper.currentUser
        usersMiddleWare.updateProfile(displayName, currentUser?.lastName, currentUser?.avatarURL)
    }

    private suspend fun connectUser(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            var job: Job? = null
            suspendCancellableCoroutine { continuation ->
                job = ConnectionEventsObserver.onChangedConnectStatusFlow.onEach {
                    when (it.state) {
                        ConnectionState.Connected -> {
                            continuation.resume(Result.success(true))
                            job?.cancel()
                        }

                        ConnectionState.Disconnected, ConnectionState.Failed -> {
                            continuation.resume(Result.failure(Exception(it.exception?.message
                                    ?: "Connection failed")))
                            job?.cancel()
                        }

                        else -> {}
                    }
                }.launchIn(this)

                connectionProvider.connectChatClient(userId)
            }
        }
    }
}