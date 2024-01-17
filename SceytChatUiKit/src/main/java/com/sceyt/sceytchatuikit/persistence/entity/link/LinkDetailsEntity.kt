package com.sceyt.sceytchatuikit.persistence.entity.link

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "LinkDetails")
data class LinkDetailsEntity(
    @PrimaryKey
    val link: String,
    val url: String?,
    val title: String?,
    val description: String?,
    var siteName: String?,
    var faviconUrl: String?,
    val imageUrl: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val thumb: String?
)