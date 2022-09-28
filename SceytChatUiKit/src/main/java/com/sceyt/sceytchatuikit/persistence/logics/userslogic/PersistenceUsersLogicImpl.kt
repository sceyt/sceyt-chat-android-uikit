package com.sceyt.sceytchatuikit.persistence.logics.userslogic

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.repositories.UsersRepository
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity

internal class PersistenceUsersLogicImpl(
        private val userDao: UserDao,
        private val userRepository: UsersRepository
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
}