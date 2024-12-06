package com.sceyt.chat.demo.presentation.main.profile.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditProfileViewModel : BaseViewModel() {
    private val userInteractor: UserInteractor by lazy { SceytChatUIKit.chatUIFacade.userInteractor }

    private val _currentUserLiveData = MutableLiveData<SceytUser>()
    val currentUserLiveData = _currentUserLiveData.asLiveData()

    private val _editProfileLiveData = MutableLiveData<SceytUser>()
    val editProfileLiveData = _editProfileLiveData.asLiveData()

    private val _editProfileErrorLiveData = MutableLiveData<String?>()
    val editProfileErrorLiveData: LiveData<String?> = _editProfileErrorLiveData

    init {
        getCurrentUser()
    }

    private fun getCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userInteractor.getCurrentUser()
            _currentUserLiveData.postValue(user ?: return@launch)
        }
    }

    fun saveProfile(
        firstName: String?,
        lastName: String?,
        username: String,
        avatarUrl: String?,
        shouldUploadAvatar: Boolean
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
            val currentUser = _currentUserLiveData.value ?: userInteractor.getCurrentUser()
            ?: return@launch
            when (val response = userInteractor.updateProfile(
                username, firstName, lastName,
                newUrl, currentUser.metadataMap
            )) {
                is SceytResponse.Success -> {
                    _editProfileLiveData.postValue(response.data ?: return@launch)
                }

                is SceytResponse.Error -> {
                    _editProfileErrorLiveData.postValue(response.message)
                }
            }
        }
    }
}