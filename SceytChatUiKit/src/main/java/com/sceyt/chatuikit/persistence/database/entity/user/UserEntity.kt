package com.sceyt.chatuikit.persistence.database.entity.user

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.messages.SceytPresence
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_TABLE

@Entity(tableName = USER_TABLE)
internal data class UserEntity(
    @ColumnInfo(name = "user_id")
    @PrimaryKey
    val id: String,
    @ColumnInfo(index = true, defaultValue = "")
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val avatarURL: String?,
    @Embedded
    val presence: SceytPresence? = null,
    val activityStatus: UserState? = null,
    val blocked: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        return (other as? UserEntity)?.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}