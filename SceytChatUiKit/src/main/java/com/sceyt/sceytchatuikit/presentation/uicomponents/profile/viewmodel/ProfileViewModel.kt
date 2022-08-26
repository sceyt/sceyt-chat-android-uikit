package com.sceyt.sceytchatuikit.presentation.uicomponents.profile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.settings.Settings
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.SceytKoinComponent
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.repositories.ProfileRepository
import com.sceyt.sceytchatuikit.persistence.SceytDatabase
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ProfileViewModel : BaseViewModel(), SceytKoinComponent {
    private val profileRepo: ProfileRepository by inject()
    private val preference: SceytSharedPreference by inject()
    private val sceytDatabase: SceytDatabase by inject()

    private val _currentUserLiveData = MutableLiveData<User>()
    val currentUserLiveData: LiveData<User> = _currentUserLiveData

    private val _editProfileLiveData = MutableLiveData<User>()
    val editProfileLiveData: LiveData<User> = _editProfileLiveData

    private val _muteUnMuteLiveData = MutableLiveData<Boolean>()
    val muteUnMuteLiveData: LiveData<Boolean> = _muteUnMuteLiveData

    private val _settingsLiveData = MutableLiveData<Settings>()
    val settingsLiveData: LiveData<Settings> = _settingsLiveData

    private val _editProfileErrorLiveData = MutableLiveData<String?>()
    val editProfileErrorLiveData: LiveData<String?> = _editProfileErrorLiveData

    fun getCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = profileRepo.getCurrentUser()
            if (response is SceytResponse.Success) {
                _currentUserLiveData.postValue(response.data ?: return@launch)
            }
        }
    }

    fun saveProfile(displayName: String, avatarUrl: String?, editedAvatar: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            var newUrl = avatarUrl
            if (editedAvatar && avatarUrl != null) {
                val uploadResult = profileRepo.uploadAvatar(avatarUrl)
                if (uploadResult is SceytResponse.Success) {
                    newUrl = uploadResult.data
                } else {
                    _editProfileErrorLiveData.postValue(uploadResult.message)
                    return@launch
                }
            }
            when (val response = profileRepo.editProfile(displayName, newUrl)) {
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
            val response = profileRepo.getSettings()
            notifyResponseAndPageState(_settingsLiveData, response)
        }
    }

    fun muteNotifications(muteUntil: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val response = profileRepo.muteNotifications(muteUntil)) {
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
            when (val response = profileRepo.unMuteNotifications()) {
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
        ChatClient.getClient().disconnect()
        preference.clear()
        sceytDatabase.clearAllTables()
        //todo unregister push token
    }
}