package com.sceyt.chatuikit.presentation.components.profile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.onError
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileViewModel(val userId: String) : BaseViewModel() {
    private val userInteractor: UserInteractor = SceytChatUIKit.chatUIFacade.userInteractor

    private val _muteUnMuteLiveData = MutableLiveData<Boolean>()
    val muteUnMuteLiveData: LiveData<Boolean> = _muteUnMuteLiveData

    private val _settingsLiveData = MutableLiveData<UserSettings>()
    val settingsLiveData: LiveData<UserSettings> = _settingsLiveData

    private val _editProfileErrorLiveData = MutableLiveData<String?>()
    val editProfileErrorLiveData: LiveData<String?> = _editProfileErrorLiveData

    private val _logOutLiveData = MutableLiveData<Boolean>()
    val logOutLiveData: LiveData<Boolean> = _logOutLiveData

    private val _currentUserAsFlow = MutableStateFlow(SceytChatUIKit.currentUser)
    val currentUserAsFlow = _currentUserAsFlow.asStateFlow()

    init {
        userInteractor.getCurrentUserAsFlow(userId)?.onEach { user ->
            _currentUserAsFlow.value = user
        }?.launchIn(viewModelScope)
    }

    fun updateProfile(
        firstName: String?,
        lastName: String?,
        username: String,
        avatarUrl: String?,
        shouldUploadAvatar: Boolean,
        metadataMap: Map<String, String>?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var newUrl = avatarUrl
            if (shouldUploadAvatar && avatarUrl != null) {
                val uploadResult = userInteractor.uploadAvatar(avatarUrl)
                if (uploadResult is SceytResponse.Success) {
                    newUrl = uploadResult.data
                } else {
                    _editProfileErrorLiveData.postValue(uploadResult.message)
                    return@launch
                }
            }

            userInteractor.updateProfile(
                username = username,
                firstName = firstName,
                lastName = lastName,
                avatarUrl = newUrl,
                metadataMap = metadataMap
            ).onError {
                _editProfileErrorLiveData.postValue(it?.message ?: return@onError)
            }
        }
    }

    fun getSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            ConnectionEventManager.awaitToConnectSceyt()
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
            SceytChatUIKit.chatUIFacade.logOut {
                _logOutLiveData.postValue(true)
            }
        }
    }
}