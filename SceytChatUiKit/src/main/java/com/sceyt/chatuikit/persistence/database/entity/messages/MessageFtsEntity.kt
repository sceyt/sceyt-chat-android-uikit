package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_FTS_TABLE

@Fts4(
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    contentEntity = MessageEntity::class,
)
@Entity(tableName = MESSAGE_FTS_TABLE)
internal data class MessageFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0,
    @ColumnInfo(name = "body")
    val body: String,
)
