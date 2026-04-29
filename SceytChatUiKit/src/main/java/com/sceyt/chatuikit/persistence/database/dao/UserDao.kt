package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_CHAT_LINK_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_METADATA_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_TABLE
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertUsersIgnored(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMetadata(list: List<UserMetadataEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertMetadataIgnored(list: List<UserMetadataEntity>)

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

        val metadata = users.flatMap { it.metadata }.takeIf { it.isNotEmpty() } ?: return
        if (replaceUserOnConflict) {
            insertMetadata(metadata)
        } else {
            insertMetadataIgnored(metadata)
        }
    }

    @Transaction
    @Query("SELECT * FROM $USER_TABLE WHERE user_id = :id")
    abstract suspend fun getUserById(id: String): UserDb?

    @Transaction
    @Query("SELECT * FROM $USER_TABLE WHERE user_id = :id")
    abstract fun getUserByIdAsFlow(id: String): Flow<UserDb?>

    @Transaction
    @Query("SELECT * FROM $USER_TABLE WHERE user_id IN (:id)")
    abstract suspend fun getUsersById(id: List<String>): List<UserDb>

    @Query(
        """
        SELECT user_id
        FROM $USER_TABLE
        WHERE firstName LIKE '%' || :searchQuery || '%'
           OR lastName LIKE '%' || :searchQuery || '%'
           OR (firstName || ' ' || lastName) LIKE :searchQuery || '%'
        """
    )
    abstract suspend fun getUserIdsByDisplayName(searchQuery: String): List<String>

    @Transaction
    @Query(
        """
        SELECT DISTINCT user.*
        FROM $USER_TABLE AS user
        JOIN $USER_CHAT_LINK_TABLE AS link ON link.user_id = user.user_id
        JOIN $CHANNEL_TABLE AS channel ON channel.chat_id = link.chat_id
        WHERE channel.userRole <> ''
          AND (:excludedUserId IS NULL OR user.user_id != :excludedUserId)
          AND (
              user.firstName LIKE '%' || :searchQuery || '%'
              OR user.lastName LIKE '%' || :searchQuery || '%'
              OR (user.firstName || ' ' || user.lastName) LIKE '%' || :searchQuery || '%'
              OR user.username LIKE '%' || :searchQuery || '%'
          )
        ORDER BY
          CASE WHEN (user.firstName || ' ' || user.lastName) LIKE :searchQuery || '%' THEN 0 ELSE 1 END,
          CASE WHEN user.username LIKE :searchQuery || '%' THEN 0 ELSE 1 END,
          user.firstName COLLATE NOCASE,
          user.lastName COLLATE NOCASE,
          user.username COLLATE NOCASE
        LIMIT :limit
        """
    )
    abstract suspend fun searchUsersLinkedToJoinedChannelsByDisplayName(
        searchQuery: String,
        excludedUserId: String?,
        limit: Int,
    ): List<UserDb>

    @Transaction
    @Query(
        """
        SELECT *
        FROM $USER_TABLE
        WHERE user_id IN (
            SELECT user_id
            FROM $USER_METADATA_TABLE
            WHERE `key` IN (:key)
              AND value LIKE '%' || :value || '%'
        )
        """
    )
    abstract suspend fun searchUsersByMetadata(key: List<String>, value: String): List<UserDb>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun updateUsers(users: List<UserEntity>)

    @Query("UPDATE $USER_TABLE SET status = :status WHERE user_id = :userId")
    abstract suspend fun updateUserStatus(userId: String, status: String)

    @Query("UPDATE $USER_TABLE SET blocked = :blocked WHERE user_id = :userId")
    abstract suspend fun blockUnBlockUser(userId: String, blocked: Boolean)
}