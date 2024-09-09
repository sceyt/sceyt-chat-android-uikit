package com.sceyt.chatuikit.presentation.uicomponents.locationpreview

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationResult(
        val snapshotPath: String,
        val latitude: Double,
        val longitude: Double
) : Parcelable