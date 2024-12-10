package com.sceyt.chat.demo.presentation.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface NavigationState {
    data object Main : NavigationState
    data object Welcome : NavigationState
}

class SplashViewModel(
    private val preference: AppSharedPreference
) : BaseViewModel() {
    private val _navigationState = MutableLiveData<NavigationState>()
    val navigationState: LiveData<NavigationState> get() = _navigationState

    init {
        checkLoginState()
    }

    private fun checkLoginState() {
        viewModelScope.launch {
            delay(1000)
            when {
                isLoggedIn() -> _navigationState.postValue(NavigationState.Main)
                else -> _navigationState.postValue(NavigationState.Welcome)
            }
        }
    }

    private fun isLoggedIn() =
        preference.getString(AppSharedPreference.PREF_USER_ID).isNullOrBlank().not()
}