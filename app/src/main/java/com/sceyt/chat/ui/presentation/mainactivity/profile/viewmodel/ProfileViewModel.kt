package com.sceyt.chat.ui.presentation.mainactivity.profile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.ProfileRepository
import com.sceyt.chat.ui.data.ProfileRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel : BaseViewModel() {
    // Todo di
    private val profileRepo: ProfileRepository = ProfileRepositoryImpl()

    private val _currentUserLiveData = MutableLiveData<User>()
    val currentUserLiveData: LiveData<User> = _currentUserLiveData

    private val _editProfileLiveData = MutableLiveData<User>()
    val editProfileLiveData: LiveData<User> = _editProfileLiveData

    private val _editProfileErrorLiveData = MutableLiveData<String?>()
    val editProfileErrorLiveData: LiveData<String?> = _editProfileErrorLiveData

    fun getCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = profileRepo.getCurrentUser()
            if (response is SceytResponse.Success) {
                _currentUserLiveData.postValue(response.data)
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
                    _editProfileLiveData.postValue(response.data)
                }
                is SceytResponse.Error -> {
                    _editProfileErrorLiveData.postValue(response.message)
                }
            }
        }
    }
}