package com.sceyt.sceytchatuikit.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.UserActivityState

@Entity(tableName = "users")
data class UserEntity(
        @ColumnInfo(name = "user_id")
        @PrimaryKey
        var id: String,
        val firstName: String?,
        val lastName: String?,
        var avatarURL: String?,
        val metadata: String?,
        @Embedded
        val presence: Presence? = null,
        val activityStatus: UserActivityState? = null,
        val blocked: Boolean = false
)