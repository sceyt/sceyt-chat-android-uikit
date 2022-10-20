package com.sceyt.sceytchatuikit.persistence.logics.userslogic

import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.settings.Settings
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.repositories.ProfileRepository
import com.sceyt.sceytchatuikit.data.repositories.UsersRepository
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.mappers.toUser
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class PersistenceUsersLogicImpl(
        private val userDao: UserDao,
        private val userRepository: UsersRepository,
        private val profileRepo: ProfileRepository,
        private val preference: SceytSharedPreference
) : PersistenceUsersLogic, SceytKoinComponent {

    override suspend fun getSceytUsers(ids: List<String>): SceytResponse<List<User>> {
        val response = userRepository.getSceytUsersByIds(ids)

        if (response is SceytResponse.Success) {
            response.data?.let {
                it.forEach { user ->
                    userDao.updateUser(user.toUserEntity())
                }
            }
        }

        return response
    }

    override suspend fun getCurrentUser(): User? {
        val clientUser = ClientWrapper.currentUser
        if (clientUser != null && clientUser.id.isNotNullOrBlank())
            return clientUser

        preference.getUserId()?.let {
            return userDao.getUserById(it)?.toUser()
        } ?: return null
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
                    continuation.resume(SceytResponse.Success(true))
                else continuation.resume(SceytResponse.Error(it.error))
            }
        }

        if (response is SceytResponse.Success)
            ClientWrapper.currentUser?.toUserEntity()?.let { userDao.updateUser(it) }


        return response
    }

    override suspend fun getSettings(): SceytResponse<Settings> {
        return profileRepo.getSettings()
    }

    override suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean> {
        return profileRepo.muteNotifications(muteUntil)
    }

    override suspend fun unMuteNotifications(): SceytResponse<Boolean> {
        return profileRepo.unMuteNotifications()
    }
}