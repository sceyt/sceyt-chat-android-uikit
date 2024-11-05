package com.sceyt.chatuikit.persistence.file_transfer

import android.os.Parcelable
import android.util.Size
import kotlinx.parcelize.Parcelize

@Parcelize
data class ThumbData(
        val key: Int,
        val filePath: String?,
        val size: Size
) : Parcelable
