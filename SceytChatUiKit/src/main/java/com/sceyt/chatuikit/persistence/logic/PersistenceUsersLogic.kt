package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.flow.Flow

interface PersistenceUsersLogic {
    suspend fun loadUsers(query: String): SceytResponse<List<SceytUser>>
    suspend fun loadMoreUsers(): SceytResponse<List<SceytUser>>
    suspend fun getSceytUsers(ids: List<String>): SceytResponse<List<SceytUser>>
    suspend fun getUserDbById(id: String): SceytUser?
    suspend fun getUsersDbByIds(id: List<String>): List<SceytUser>
    suspend fun getCurrentUser(): SceytUser?
    fun getCurrentUserNonSuspend(): SceytUser?
    fun getCurrentUserAsFlow(): Flow<SceytUser>
    suspend fun uploadAvatar(avatarUrl: String): SceytResponse<String>
    suspend fun updateProfile(firstName: String?, lastName: String?,
                              avatarUri: String?, metaData: String?): SceytResponse<SceytUser>

    suspend fun setPresenceState(presenceState: PresenceState): SceytResponse<Boolean>
    suspend fun updateStatus(status: String): SceytResponse<Boolean>
    suspend fun getSettings(): SceytResponse<UserSettings>
    suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean>
    suspend fun unMuteNotifications(): SceytResponse<Boolean>
    suspend fun onUserPresenceChanged(users: List<SceytPresenceChecker.PresenceUser>)
    suspend fun blockUnBlockUser(userId: String, block: Boolean): SceytResponse<List<SceytUser>>
}