package com.sceyt.sceytchatuikit.persistence.logics.userslogic

import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.repositories.ProfileRepository
import com.sceyt.sceytchatuikit.data.repositories.UsersRepository
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import com.sceyt.sceytchatuikit.persistence.mappers.toUser
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine

internal class PersistenceUsersLogicImpl(
        private val userDao: UserDao,
        private val userRepository: UsersRepository,
        private val profileRepo: ProfileRepository,
        private val preference: SceytSharedPreference
) : PersistenceUsersLogic, SceytKoinComponent {

    override suspend fun loadUsers(query: String): SceytResponse<List<User>> {
        val response = userRepository.loadUsers(query)

        if (response is SceytResponse.Success) {
            response.data?.let { users ->
                userDao.updateUsers(users.map { it.toUserEntity() })
            }
        }

        return response
    }

    override suspend fun loadMoreUsers(): SceytResponse<List<User>> {
        val response = userRepository.loadMoreUsers()

        if (response is SceytResponse.Success) {
            response.data?.let { users ->
                userDao.updateUsers(users.map { it.toUserEntity() })
            }
        }

        return response
    }

    override suspend fun getSceytUsers(ids: List<String>): SceytResponse<List<User>> {
        val response = userRepository.getSceytUsersByIds(ids)

        if (response is SceytResponse.Success) {
            response.data?.let { users ->
                userDao.updateUsers(users.map { it.toUserEntity() })
            }
        }

        return response
    }

    override suspend fun getUserDbById(id: String): User? {
        return userDao.getUserById(id)?.toUser()
    }

    override suspend fun getUsersDbByIds(id: List<String>): List<User> {
        return userDao.getUsersById(id).map { it.toUser() }
    }

    override suspend fun getCurrentUser(): User? {
        var clientUser = ClientWrapper.currentUser
        if (clientUser?.id.isNullOrBlank())
            clientUser = null

        return preference.getUserId()?.let {
            userDao.getUserById(it)?.toUser()
        } ?: clientUser
    }

    override fun getCurrentUserAsFlow(): Flow<User> {
        (preference.getUserId() ?: ClientWrapper.currentUser?.id)?.let {
            return userDao.getUserByIdAsFlow(it).filterNotNull().map { entity -> entity.toUser() }
        } ?: return emptyFlow()
    }

    override suspend fun uploadAvatar(avatarUrl: String): SceytResponse<String> {
        return profileRepo.uploadAvatar(avatarUrl)
    }

    override suspend fun updateProfile(firstName: String?, lastName: String?, avatarUri: String?): SceytResponse<User> {
        val response = profileRepo.updateProfile(firstName, lastName, avatarUri)

        if (response is SceytResponse.Success) {
            response.data?.let {
                userDao.updateUser(it.toUserEntity())
            }
        }

        return response
    }

    override suspend fun updateStatus(status: String): SceytResponse<Boolean> {
        val presence = getCurrentUser()?.presence?.state ?: PresenceState.Offline

        val response = suspendCancellableCoroutine<SceytResponse<Boolean>> { continuation ->
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

    override suspend fun setPresenceState(presenceState: PresenceState): SceytResponse<Boolean> {
        val status = ClientWrapper.currentUser?.presence?.status
        val response = suspendCancellableCoroutine<SceytResponse<Boolean>> { continuation ->
            ClientWrapper.setPresence(presenceState, if (status.isNullOrBlank())
                SceytKitConfig.presenceStatusText else status) {
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

    private suspend fun updateCurrentUser() {
        (preference.getUserId() ?: ClientWrapper.currentUser?.id)?.let {
            val response = userRepository.getSceytUserById(it)
            if (response is SceytResponse.Success)
                response.data?.toUserEntity()?.let { entity -> userDao.insertUser(entity) }
        }
    }
}