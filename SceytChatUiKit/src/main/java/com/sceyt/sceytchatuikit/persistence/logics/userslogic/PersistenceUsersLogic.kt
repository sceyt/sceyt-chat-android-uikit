package com.sceyt.sceytchatuikit.persistence.logics.userslogic

import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.flow.Flow

interface PersistenceUsersLogic {
    suspend fun loadUsers(query: String): SceytResponse<List<User>>
    suspend fun loadMoreUsers(): SceytResponse<List<User>>
    suspend fun getSceytUsers(ids: List<String>): SceytResponse<List<User>>
    suspend fun getUserDbById(id: String): User?
    suspend fun getUsersDbByIds(id: List<String>): List<User>
    suspend fun getCurrentUser(): User?
    fun getCurrentUserAsFlow(): Flow<User>
    suspend fun uploadAvatar(avatarUrl: String): SceytResponse<String>
    suspend fun updateProfile(firstName: String?, lastName: String?, avatarUri: String?): SceytResponse<User>
    suspend fun setPresenceState(presenceState: PresenceState): SceytResponse<Boolean>
    suspend fun updateStatus(status: String): SceytResponse<Boolean>
    suspend fun getSettings(): SceytResponse<UserSettings>
    suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean>
    suspend fun unMuteNotifications(): SceytResponse<Boolean>
    suspend fun onUserPresenceChanged(users: List<SceytPresenceChecker.PresenceUser>)
}