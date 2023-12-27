package com.sceyt.sceytchatuikit.persistence.entity.link

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LinkDetailsEntity(
    @PrimaryKey
    val link: String,
    val messageTid: Long,
    val url: String?,
    val description: String?,
    var siteName: String?,
    var faviconUrl: String?,
    val imageUrl: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
)