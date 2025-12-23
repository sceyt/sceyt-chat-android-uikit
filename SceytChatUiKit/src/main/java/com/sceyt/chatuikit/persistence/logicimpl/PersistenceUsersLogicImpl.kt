package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserListQuery
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.repositories.getUserId
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceUsersLogic
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.persistence.mappers.toUser
import com.sceyt.chatuikit.persistence.mappers.toUserDb
import com.sceyt.chatuikit.persistence.mappers.toUserEntity
import com.sceyt.chatuikit.persistence.repositories.ProfileRepository
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.persistence.repositories.UsersRepository
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject

internal class PersistenceUsersLogicImpl(
    private val userDao: UserDao,
    private val userRepository: UsersRepository,
    private val profileRepo: ProfileRepository,
    private val preference: SceytSharedPreference,
) : PersistenceUsersLogic, SceytKoinComponent {
    private val persistenceChannelsLogic: PersistenceChannelsLogic by inject()

    override suspend fun loadUsers(query: UserListQuery): SceytResponse<List<SceytUser>> {
        val response = userRepository.loadUsers(query)

        if (response is SceytResponse.Success) {
            response.data?.let { users ->
                userDao.insertUsersWithMetadata(users.map { it.toUserDb() })
            }
        }

        return response
    }

    override suspend fun getUserById(id: String): SceytResponse<SceytUser> {
        val response = userRepository.getUserById(id)

        if (response is SceytResponse.Success) {
            response.data?.let { user ->
                userDao.insertUserWithMetadata(user.toUserDb())
            }
        }

        return response
    }

    override suspend fun loadMoreUsers(): SceytResponse<List<SceytUser>> {
        val response = userRepository.loadMoreUsers()

        if (response is SceytResponse.Success) {
            response.data?.let { users ->
                userDao.insertUsersWithMetadata(users.map { it.toUserDb() })
            }
        }

        return response
    }

    override suspend fun getUsersByIds(ids: List<String>): SceytResponse<List<SceytUser>> {
        val response = userRepository.getUsersByIds(ids)

        if (response is SceytResponse.Success) {
            response.data?.let { users ->
                userDao.insertUsersWithMetadata(users.map { it.toUserDb() })
            }
        }

        return response
    }

    override suspend fun getUserFromDbById(id: String): SceytUser? {
        return userDao.getUserById(id)?.toSceytUser()
    }

    override suspend fun getUsersFromDbByIds(id: List<String>): List<SceytUser> {
        return userDao.getUsersById(id).map { it.toSceytUser() }
    }

    override suspend fun searchLocaleUserByMetadata(
        metadataKeys: List<String>,
        metadataValue: String,
    ): List<SceytUser> {
        return userDao.searchUsersByMetadata(metadataKeys, metadataValue).map { it.toSceytUser() }
    }

    override suspend fun getCurrentUser(refreshFromServer: Boolean): SceytUser? {
        return if (refreshFromServer) {
            userRepository.getUserById(
                SceytChatUIKit.currentUserId ?: return null
            ).fold(
                onSuccess = { user ->
                    user?.also {
                        userDao.insertUserWithMetadata(it.toUserDb())
                        ClientWrapper.currentUser = it.toUser()
                    }
                },
                onError = { null }
            )
        } else {
            val clientUser = ClientWrapper.currentUser
            if (!clientUser?.id.isNullOrBlank())
                clientUser.toSceytUser()
            else preference.getUserId()?.let {
                userDao.getUserById(it)?.toSceytUser()
            }
        }
    }

    override fun getCurrentUserId(): String? {
        val clientUser = ClientWrapper.currentUser
        if (!clientUser?.id.isNullOrBlank())
            return clientUser.id

        return preference.getUserId()
    }

    override fun getCurrentUserAsFlow(currentUserId: String?): Flow<SceytUser>? {
        val userId = currentUserId ?: getCurrentUserId() ?: return null
        return userDao.getUserByIdAsFlow(userId)
            .filterNotNull()
            .map { userDb -> userDb.toSceytUser() }
            .distinctUntilChanged { old, new -> !old.diff(new).hasDifference() }
    }

    override suspend fun uploadAvatar(avatarUrl: String): SceytResponse<String> {
        return profileRepo.uploadAvatar(avatarUrl)
    }

    override suspend fun updateProfile(
        username: String,
        firstName: String?,
        lastName: String?,
        avatarUri: String?,
        metadataMap: Map<String, String>?,
    ): SceytResponse<SceytUser> {
        val request = User.setProfileRequest().apply {
            avatarUri?.let { uri ->
                setAvatar(uri)
            }
            setUsername(username)
            setFirstName(firstName ?: "")
            setLastName(lastName ?: "")
            setMetadataMap(metadataMap ?: emptyMap())
        }
        val response = profileRepo.updateProfile(request)

        if (response is SceytResponse.Success) {
            response.data?.let {
                userDao.insertUserWithMetadata(it.toUserDb())
            }
        }

        return response
    }

    override suspend fun updateStatus(status: String): SceytResponse<Boolean> {
        val presence = getCurrentUser(false)?.presence?.state ?: PresenceState.Offline

        val response = suspendCancellableCoroutine { continuation ->
            ClientWrapper.setPresence(presence, status) {
                if (it.isOk)
                    continuation.safeResume(SceytResponse.Success(true))
                else continuation.safeResume(SceytResponse.Error(it.error))
            }
        }

        if (response is SceytResponse.Success)
            preference.getUserId()?.let { userDao.updateUserStatus(it, status) }

        return response
    }

    override suspend fun setPresenceState(
        presenceState: PresenceState
    ): SceytResponse<Boolean> {
        val status = ClientWrapper.currentUser?.presence?.status
        val response = suspendCancellableCoroutine { continuation ->
            ClientWrapper.setPresence(
                presenceState, if (status.isNullOrBlank())
                    SceytChatUIKit.config.presenceConfig.defaultPresenceStatus else status
            ) {
                if (it.isOk) {
                    continuation.safeResume(SceytResponse.Success(true))
                } else continuation.safeResume(SceytResponse.Error(it.error))
            }
        }

        if (response is SceytResponse.Success)
            updateCurrentUser()

        return response
    }

    override suspend fun getSettings(): SceytResponse<UserSettings> {
        return profileRepo.getSettings()
    }

    override suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean> {
        return profileRepo.muteNotifications(muteUntil)
    }

    override suspend fun unMuteNotifications(): SceytResponse<Boolean> {
        return profileRepo.unMuteNotifications()
    }

    override suspend fun onUserPresenceChanged(users: List<SceytPresenceChecker.PresenceUser>) {
        userDao.updateUsers(users.map { it.user.toUserEntity() })
    }

    override suspend fun blockUnBlockUser(
        userId: String,
        block: Boolean
    ): SceytResponse<List<SceytUser>> {
        val response = if (block) {
            userRepository.blockUser(userId)
        } else
            userRepository.unblockUser(userId)

        if (response is SceytResponse.Success) {
            userDao.blockUnBlockUser(userId, block)
            persistenceChannelsLogic.blockUnBlockUser(userId, block)
        }

        return response
    }

    private suspend fun updateCurrentUser() {
        (preference.getUserId() ?: ClientWrapper.currentUser?.id)?.let {
            val response = userRepository.getUserById(it)
            if (response is SceytResponse.Success)
                response.data?.toUserDb()?.let { userDb ->
                    userDao.insertUserWithMetadata(userDb)
                }
        }
    }
}