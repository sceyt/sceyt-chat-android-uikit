package com.sceyt.chat.demo.presentation.welcome.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.connection.SceytChatConnectionManager
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.root.PageState
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class WelcomeViewModel(
    private val preference: AppSharedPreference,
    private val connectionManager: SceytChatConnectionManager
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
    ): Result<Unit> = connectionManager.connectAndAwait(
        userId = userId,
        timeoutMillis = 8.seconds.inWholeMilliseconds
    )
}
