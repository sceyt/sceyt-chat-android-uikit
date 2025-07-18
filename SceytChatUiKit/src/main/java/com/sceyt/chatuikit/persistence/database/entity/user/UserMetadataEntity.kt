package com.sceyt.chatuikit.persistence.database.entity.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_METADATA_TABLE

@Entity(
    tableName = USER_METADATA_TABLE,
    primaryKeys = ["user_id", "key"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ]
)
internal data class UserMetadataEntity(
        @ColumnInfo(name = "user_id")
        val userId: String,
        val key: String,
        val value: String
)