package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("select * from users where user_id =:id")
    suspend fun getUserById(id: String): UserEntity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUser(user: UserEntity)

    @Query("update users set status =:status where user_id =:userId")
    suspend fun updateUserStatus(userId: String, status: String)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUsers(users: List<UserEntity>)

    @Query("update users set blocked =:blocked where user_id =:userId")
    suspend fun blockUnBlockUser(userId: String, blocked: Boolean)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}