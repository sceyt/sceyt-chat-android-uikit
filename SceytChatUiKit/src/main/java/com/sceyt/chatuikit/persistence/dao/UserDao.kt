package com.sceyt.chatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sceyt.chatuikit.persistence.entity.user.UserDb
import com.sceyt.chatuikit.persistence.entity.user.UserEntity
import com.sceyt.chatuikit.persistence.entity.user.UserMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(list: List<UserMetadataEntity>)

    @Transaction
    suspend fun insertUserWithMetadata(user: UserDb) {
        insertUser(user.user)
        insertMetadata(user.metadata)
    }

    @Transaction
    suspend fun insertUsersWithMetadata(users: List<UserDb>) {
        insertUsers(users.map { it.user })
        insertMetadata(users.flatMap { it.metadata })
    }

    @Transaction
    @Query("select * from users where user_id =:id")
    suspend fun getUserById(id: String): UserDb?

    @Transaction
    @Query("select * from users where user_id =:id")
    fun getUserByIdAsFlow(id: String): Flow<UserDb?>

    @Transaction
    @Query("select * from users where user_id in (:id)")
    suspend fun getUsersById(id: List<String>): List<UserDb>

    @Query("select user_id from users where firstName like '%' || :searchQuery || '%' " +
            "or lastName like  '%' || :searchQuery || '%' or (firstName || ' ' || lastName) like :searchQuery || '%'")
    suspend fun getUserIdsByDisplayName(searchQuery: String): List<String>

    @Transaction
    @Query("""
            select * from users where user_id in (
            select user_id from UserMetadata
            where `key` in (:key) and value like '%' || :value || '%')
           """
    )
    suspend fun searchUsersByMetadata(key: List<String>, value: String): List<UserDb>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUser(user: UserEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUsers(users: List<UserEntity>)

    @Query("update users set status =:status where user_id =:userId")
    suspend fun updateUserStatus(userId: String, status: String)

    @Query("update users set blocked =:blocked where user_id =:userId")
    suspend fun blockUnBlockUser(userId: String, blocked: Boolean)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}