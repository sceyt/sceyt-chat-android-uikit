package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sceyt.chatuikit.persistence.database.entity.user.USER_METADATA_TABLE
import com.sceyt.chatuikit.persistence.database.entity.user.USER_TABLE
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertUsersIgnored(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMetadata(list: List<UserMetadataEntity>)

    @Transaction
    open suspend fun insertUserWithMetadata(user: UserDb) {
        insertUser(user.user)
        insertMetadata(user.metadata)
    }

    @Transaction
    open suspend fun insertUsersWithMetadata(
            users: List<UserDb>,
            replaceUserOnConflict: Boolean = true
    ) {
        if (users.isEmpty()) return
        if (replaceUserOnConflict) {
            insertUsers(users.map { it.user })
        } else {
            insertUsersIgnored(users.map { it.user })
        }
        users.flatMap { it.metadata }.takeIf { it.isNotEmpty() }?.let {
            insertMetadata(it)
        }
    }

    @Transaction
    @Query("select * from $USER_TABLE  where user_id =:id")
    abstract suspend fun getUserById(id: String): UserDb?

    @Transaction
    @Query("select * from $USER_TABLE  where user_id =:id")
    abstract fun getUserByIdAsFlow(id: String): Flow<UserDb?>

    @Transaction
    @Query("select * from $USER_TABLE  where user_id in (:id)")
    abstract suspend fun getUsersById(id: List<String>): List<UserDb>

    @Query("""
           select user_id from $USER_TABLE  where 
           firstName like '%' || :searchQuery || '%' 
           or lastName like  '%' || :searchQuery || '%'
           or (firstName || ' ' || lastName) like :searchQuery || '%'
           """)
    abstract suspend fun getUserIdsByDisplayName(searchQuery: String): List<String>

    @Transaction
    @Query("""
           select * from $USER_TABLE  where user_id in (
           select user_id from $USER_METADATA_TABLE
           where `key` in (:key) and value like '%' || :value || '%')
           """
    )
    abstract suspend fun searchUsersByMetadata(key: List<String>, value: String): List<UserDb>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun updateUser(user: UserEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun updateUsers(users: List<UserEntity>)

    @Query("update $USER_TABLE set status =:status where user_id =:userId")
    abstract suspend fun updateUserStatus(userId: String, status: String)

    @Query("update $USER_TABLE set blocked =:blocked where user_id =:userId")
    abstract suspend fun blockUnBlockUser(userId: String, blocked: Boolean)

    @Query("DELETE from $USER_TABLE ")
    abstract suspend fun deleteAll()
}