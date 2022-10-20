package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.settings.Settings
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.SceytResponse

interface PersistenceUsersMiddleWare {
    suspend fun getUsersByIds(ids: List<String>): SceytResponse<List<User>>
    suspend fun getCurrentUser(): User?
    suspend fun uploadAvatar(avatarUrl: String): SceytResponse<String>
    suspend fun updateProfile(firsName: String?, lastName: String?, avatarUrl: String?): SceytResponse<User>
    suspend fun updateStatus(status: String): SceytResponse<Boolean>
    suspend fun getSettings(): SceytResponse<Settings>
    suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean>
    suspend fun unMuteNotifications(): SceytResponse<Boolean>
}