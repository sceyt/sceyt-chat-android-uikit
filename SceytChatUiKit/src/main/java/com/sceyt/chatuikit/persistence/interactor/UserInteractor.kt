package com.sceyt.chatuikit.persistence.interactor

import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.UserListQuery
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser
import kotlinx.coroutines.flow.Flow

interface UserInteractor {
    suspend fun loadUsers(query: UserListQuery): SceytResponse<List<SceytUser>>
    suspend fun loadMoreUsers(): SceytResponse<List<SceytUser>>
    suspend fun getUserById(id: String): SceytResponse<SceytUser>
    suspend fun getUsersByIds(ids: List<String>): SceytResponse<List<SceytUser>>
    suspend fun getUserFromDbById(id: String): SceytUser?
    suspend fun getUsersFromDbByIds(id: List<String>): List<SceytUser>
    suspend fun searchLocaleUserByMetadata(
        metadataKeys: List<String>,
        metadataValue: String
    ): List<SceytUser>

    suspend fun getCurrentUser(refreshFromServer: Boolean = false): SceytUser?
    fun getCurrentUserId(): String?
    fun getCurrentUserAsFlow(currentUserId: String? = getCurrentUserId()): Flow<SceytUser>?
    suspend fun uploadAvatar(avatarUrl: String): SceytResponse<String>
    suspend fun updateProfile(
        username: String, firstName: String?, lastName: String?,
        avatarUrl: String?, metadataMap: Map<String, String>?
    ): SceytResponse<SceytUser>

    suspend fun setPresenceState(presenceState: PresenceState): SceytResponse<Boolean>
    suspend fun updateStatus(status: String): SceytResponse<Boolean>
    suspend fun getSettings(): SceytResponse<UserSettings>
    suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean>
    suspend fun unMuteNotifications(): SceytResponse<Boolean>
    suspend fun blockUnBlockUser(userId: String, block: Boolean): SceytResponse<List<SceytUser>>
}