package com.sceyt.chatuikit.presentation.uicomponents.profile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ProfileViewModel : BaseViewModel(), SceytKoinComponent {
    private val userInteractor: UserInteractor by inject()

    private val _currentUserLiveData = MutableLiveData<User>()
    val currentUserLiveData: LiveData<User> = _currentUserLiveData

    private val _editProfileLiveData = MutableLiveData<User>()
    val editProfileLiveData: LiveData<User> = _editProfileLiveData

    private val _muteUnMuteLiveData = MutableLiveData<Boolean>()
    val muteUnMuteLiveData: LiveData<Boolean> = _muteUnMuteLiveData

    private val _settingsLiveData = MutableLiveData<UserSettings>()
    val settingsLiveData: LiveData<UserSettings> = _settingsLiveData

    private val _editProfileErrorLiveData = MutableLiveData<String?>()
    val editProfileErrorLiveData: LiveData<String?> = _editProfileErrorLiveData

    private val _logOutLiveData = MutableLiveData<Boolean>()
    val logOutLiveData: LiveData<Boolean> = _logOutLiveData

    private val _logOutErrorLiveData = MutableLiveData<String>()
    val logOutErrorLiveData: LiveData<String> = _logOutErrorLiveData

    fun getCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userInteractor.getCurrentUser()
            _currentUserLiveData.postValue(user ?: return@launch)
        }
    }

    fun saveProfile(firstName: String?, lastName: String?, avatarUrl: String?, editedAvatar: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            var newUrl = avatarUrl
            if (editedAvatar && avatarUrl != null) {
                val uploadResult = userInteractor.uploadAvatar(avatarUrl)
                if (uploadResult is SceytResponse.Success) {
                    newUrl = uploadResult.data
                } else {
                    _editProfileErrorLiveData.postValue(uploadResult.message)
                    return@launch
                }
            }
            when (val response = userInteractor.updateProfile(firstName, lastName, newUrl, null)) {
                is SceytResponse.Success -> {
                    _editProfileLiveData.postValue(response.data ?: return@launch)
                }

                is SceytResponse.Error -> {
                    _editProfileErrorLiveData.postValue(response.message)
                }
            }
        }
    }

    fun getSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            ConnectionEventsObserver.awaitToConnectSceyt()
            val response = userInteractor.getSettings()
            notifyResponseAndPageState(_settingsLiveData, response)
        }
    }

    fun muteNotifications(muteUntil: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val response = userInteractor.muteNotifications(muteUntil)) {
                is SceytResponse.Success -> {
                    _muteUnMuteLiveData.postValue(true)
                }

                is SceytResponse.Error -> {
                    _muteUnMuteLiveData.postValue(false)
                    notifyPageStateWithResponse(response)
                }
            }
        }
    }

    fun unMuteNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val response = userInteractor.unMuteNotifications()) {
                is SceytResponse.Success -> {
                    _muteUnMuteLiveData.postValue(false)
                }

                is SceytResponse.Error -> {
                    _muteUnMuteLiveData.postValue(true)
                    notifyPageStateWithResponse(response)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            SceytChatUIKit.chatUIFacade.logOut { success, errorMessage ->
                if (success)
                    _logOutLiveData.postValue(true)
                else _logOutErrorLiveData.postValue(errorMessage ?: "")
            }
        }
    }
}