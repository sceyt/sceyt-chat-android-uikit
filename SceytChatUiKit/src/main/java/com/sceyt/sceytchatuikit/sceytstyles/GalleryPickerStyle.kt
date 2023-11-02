package com.sceyt.sceytchatuikit.sceytstyles

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

object GalleryPickerStyle {
    @ColorRes
    var nextButtonColor: Int = SceytKitConfig.sceytColorAccent

    @ColorRes
    var counterColor: Int = SceytKitConfig.sceytColorAccent

    @DrawableRes
    var checkedStateIcon: Int = R.drawable.sceyt_ic_gallery_checked_state

    @DrawableRes
    var unCheckedStateIcon: Int = R.drawable.sceyt_ic_gallery_unchecked_state

    var maxSelectCount: Int = 20
}