package com.sceyt.chat.ui.presentation.login.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.ProfileRepository
import com.sceyt.chat.ui.data.ProfileRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import kotlinx.coroutines.launch

class LoginViewModel : BaseViewModel() {
    private val profileRepository: ProfileRepository = ProfileRepositoryImpl()

    private val _editNameLiveData = MutableLiveData<User>()
    val editNameLiveData: LiveData<User> = _editNameLiveData

    fun updateDisplayName(displayName: String) {
        viewModelScope.launch {
            val response = profileRepository.getCurrentUser()
            if (response is SceytResponse.Success) {
                response.data?.let {
                    profileRepository.editProfile(displayName, it.avatarURL)
                    notifyResponseAndPageState(_editNameLiveData, profileRepository.editProfile(displayName, it.avatarURL))
                }
            } else
                notifyPageStateWithResponse(response)
        }
    }
}