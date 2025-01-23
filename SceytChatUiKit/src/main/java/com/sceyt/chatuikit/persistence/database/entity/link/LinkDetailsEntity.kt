package com.sceyt.chatuikit.persistence.database.entity.link

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "LinkDetails")
data class LinkDetailsEntity(
    @PrimaryKey
    val link: String,
    val url: String?,
    val title: String?,
    val description: String?,
    val siteName: String?,
    val faviconUrl: String?,
    val imageUrl: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val thumb: String?
)