package com.sceyt.chatuikit.persistence.entity.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE

@Entity(
    tableName = "UserMetadata",
    primaryKeys = ["user_id", "key"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = CASCADE
        )
    ]
)
data class UserMetadataEntity(
        @ColumnInfo(name = "user_id")
        val userId: String,
        val key: String,
        val value: String
)