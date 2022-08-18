package com.sceyt.chat.ui.presentation.login.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.repositories.ProfileRepository
import com.sceyt.chat.ui.data.repositories.ProfileRepositoryImpl
import com.sceyt.chat.ui.persistence.dao.UserDao
import com.sceyt.chat.ui.persistence.mappers.toUserEntity
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LoginViewModel : BaseViewModel(), KoinComponent {
    private val profileRepository: ProfileRepository = ProfileRepositoryImpl()

    //ToDo
    private val userDao: UserDao by inject()

    private
    val _editNameLiveData = MutableLiveData<User>()
    val editNameLiveData: LiveData<User> = _editNameLiveData

    fun updateDisplayName(displayName: String) {
        viewModelScope.launch {
            val response = profileRepository.getCurrentUser()
            if (response is SceytResponse.Success) {
                response.data?.let {
                    val editProfileResp = profileRepository.editProfile(displayName, it.avatarURL)
                    if (editProfileResp is SceytResponse.Success) {
                        editProfileResp.data?.let { user ->
                            userDao.insertUser(user.toUserEntity())
                        }
                    }
                    notifyResponseAndPageState(_editNameLiveData, editProfileResp)
                }
            } else
                notifyPageStateWithResponse(response)
        }
    }
}