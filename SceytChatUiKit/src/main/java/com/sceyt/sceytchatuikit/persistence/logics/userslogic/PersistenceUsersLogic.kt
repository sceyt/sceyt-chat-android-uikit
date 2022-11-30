package com.sceyt.sceytchatuikit.persistence.logics.userslogic

import com.sceyt.chat.models.settings.Settings
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.SceytResponse

interface PersistenceUsersLogic {
    suspend fun getSceytUsers(ids: List<String>): SceytResponse<List<User>>
    suspend fun getUserDbById(id: String): User?
    suspend fun getCurrentUser(): User?
    suspend fun uploadAvatar(avatarUrl: String): SceytResponse<String>
    suspend fun updateProfile(firstName: String?, lastName: String?, avatarUri: String?): SceytResponse<User>
    suspend fun updateStatus(status: String): SceytResponse<Boolean>
    suspend fun getSettings(): SceytResponse<Settings>
    suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean>
    suspend fun unMuteNotifications(): SceytResponse<Boolean>
}