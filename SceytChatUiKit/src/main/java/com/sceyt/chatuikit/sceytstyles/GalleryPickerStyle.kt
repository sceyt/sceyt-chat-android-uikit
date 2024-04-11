package com.sceyt.chatuikit.sceytstyles

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig

object GalleryPickerStyle {

    @JvmField
    @ColorRes
    var nextButtonColor: Int = SceytKitConfig.sceytColorAccent

    @JvmField
    @ColorRes
    var counterColor: Int = SceytKitConfig.sceytColorAccent

    @JvmField
    @DrawableRes
    var checkedStateIcon: Int = R.drawable.sceyt_ic_gallery_checked_state

    @JvmField
    @DrawableRes
    var unCheckedStateIcon: Int = R.drawable.sceyt_ic_gallery_unchecked_state

    @JvmField
    var maxSelectCount: Int = 20

    @JvmField
    @DrawableRes
    var videoDurationIcon: Int = R.drawable.sceyt_ic_video
}