package com.sceyt.chat.demo.presentation.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.Constants
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
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

class LoginViewModel(private val preference: AppSharedPreference,
                     private val connectionProvider: SceytConnectionProvider) : BaseViewModel() {

    private val userInteractor: UserInteractor by lazy { SceytChatUIKit.chatUIFacade.userInteractor }
    private val _logInLiveData = MutableLiveData<Boolean>()
    val logInLiveData: LiveData<Boolean> = _logInLiveData

    fun loginUser(userId: String, displayName: String) {
        notifyPageLoadingState(false)
        viewModelScope.launch {
            val result = connectUser(userId)
            if (result.isSuccess) {
                preference.setString(AppSharedPreference.PREF_USER_ID, userId)
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
                preference.setString(AppSharedPreference.PREF_USER_ID, randomUserId)
            } else
                pageStateLiveDataInternal.value = PageState.StateError(null, result.exceptionOrNull()?.message
                        ?: "Connection failed")

            pageStateLiveDataInternal.value = PageState.StateLoading(false)
            _logInLiveData.value = result.isSuccess
        }
    }

    fun isLoggedIn() = preference.getString(AppSharedPreference.PREF_USER_ID).isNullOrBlank().not()

    private suspend fun updateProfile(displayName: String) = withContext(Dispatchers.IO) {
        val currentUser = SceytChatUIKit.currentUser ?: userInteractor.getCurrentUser()
        ?: return@withContext SceytResponse.Error<SceytUser>(SceytException(0, "User not found"))
        userInteractor.updateProfile(currentUser.username, displayName, currentUser.lastName,
            currentUser.avatarURL,
            currentUser.metadataMap
        )
    }

    private suspend fun connectUser(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            var job: Job? = null
            suspendCancellableCoroutine { continuation ->
                job = ConnectionEventManager.onChangedConnectStatusFlow.onEach {
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