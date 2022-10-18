package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsers(users: List<UserEntity>)

    @Query("select * from users where user_id =:id")
    fun getUserById(id: String): UserEntity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUser(user: UserEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateUsers(users: List<UserEntity>)

    @Query("update users set blocked =:blocked where user_id =:userId")
    fun blockUnBlockUser(userId: String, blocked: Boolean)

    @Query("DELETE FROM users")
    fun deleteAll()
}