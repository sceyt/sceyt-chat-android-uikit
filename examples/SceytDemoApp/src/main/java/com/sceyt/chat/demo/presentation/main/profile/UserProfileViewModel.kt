package com.sceyt.chat.demo.presentation.main.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.data.Constants
import com.sceyt.chat.demo.data.repositories.UserRepository
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.launch

class UserProfileViewModel(
        private val userRepository: UserRepository
) : BaseViewModel() {
    private val _deleteUserErrorLiveData = MutableLiveData<String?>()
    val deleteUserErrorLiveData = _deleteUserErrorLiveData.asLiveData()

    private val myId = SceytChatUIKit.currentUserId ?: ""

    val isDemoUser = Constants.users.contains(myId)

    private fun deleteUser(logout: () -> Unit) {
        viewModelScope.launch {
            val result = userRepository.deleteUser(myId)
            when {
                result.isSuccess -> logout()
                else -> {
                    val exception = result.exceptionOrNull()
                    if (exception is SceytException)
                        _deleteUserErrorLiveData.postValue(exception.message)
                }
            }
        }
    }

    fun logout(needDeleteUser: Boolean, logout: () -> Unit) = when {
        needDeleteUser -> deleteUser(logout)
        else -> logout()
    }
}