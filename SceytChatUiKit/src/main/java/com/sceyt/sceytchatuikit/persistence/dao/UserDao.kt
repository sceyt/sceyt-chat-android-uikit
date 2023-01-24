package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("select * from users where user_id =:id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("select * from users where user_id =:id")
    fun getUserByIdAsFlow(id: String): Flow<UserEntity?>

    @Query("select * from users where user_id in (:id)")
    suspend fun getUsersById(id: List<String>): List<UserEntity>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUser(user: UserEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUsers(users: List<UserEntity>)

    suspend fun updatePresence(userId: String, presence: Presence) {
        updatePresence(userId, presence.state, presence.status, presence.lastActiveAt)
    }

    @Query("update users set state =:state, status =:status, lastActiveAt =:lastActiveAt where user_id =:userId")
    suspend fun updatePresence(userId: String, state: PresenceState, status: String, lastActiveAt: Long)

    @Query("update users set status =:status where user_id =:userId")
    suspend fun updateUserStatus(userId: String, status: String)

    @Query("update users set blocked =:blocked where user_id =:userId")
    suspend fun blockUnBlockUser(userId: String, blocked: Boolean)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}