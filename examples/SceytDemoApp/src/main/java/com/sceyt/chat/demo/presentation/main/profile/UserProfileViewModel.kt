package com.sceyt.chat.demo.presentation.main.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.Constants
import com.sceyt.chat.demo.data.repositories.UserRepository
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.launch

class UserProfileViewModel(
        private val userRepository: UserRepository,
        private val sharedPreference: AppSharedPreference
) : BaseViewModel() {
    private val _deleteUserErrorLiveData = MutableLiveData<String?>()
    val deleteUserErrorLiveData = _deleteUserErrorLiveData.asLiveData()

    private val myId = SceytChatUIKit.currentUserId ?: ""

    val isDemoUser get() = Constants.users.contains(myId)

    private fun deleteUser(logout: () -> Unit) {
        viewModelScope.launch {
            val result = userRepository.deleteUser(myId)
            if (result.isSuccess) {
                deleteCurrentUserId()
                logout()
            } else {
                val exception = result.exceptionOrNull()
                if (exception is SceytException)
                    _deleteUserErrorLiveData.postValue(exception.message)
            }
        }
    }

    fun logout(needDeleteUser: Boolean, logout: () -> Unit) =
            if (needDeleteUser) {
                deleteUser(logout)
            } else {
                if (!isDemoUser) saveCurrentUserId()
                logout()
            }

    private fun updateUserIds(action: (MutableList<String>) -> Unit) {
        val userIds = sharedPreference.getList(
            AppSharedPreference.PREF_USER_IDS,
            String::class.java
        )?.toMutableList() ?: mutableListOf()

        action(userIds)
        sharedPreference.putList(AppSharedPreference.PREF_USER_IDS, userIds)
    }

    private fun saveCurrentUserId() {
        updateUserIds { userIds ->
            if (!userIds.contains(myId)) {
                userIds.add(myId)
            }
        }
    }

    private fun deleteCurrentUserId() {
        updateUserIds { userIds ->
            if (userIds.isNotEmpty() && userIds.contains(myId)) {
                userIds.remove(myId)
            }
        }
    }
}