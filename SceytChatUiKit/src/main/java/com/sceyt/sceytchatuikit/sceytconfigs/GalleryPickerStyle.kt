package com.sceyt.sceytchatuikit.sceytconfigs

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sceyt.sceytchatuikit.R

object GalleryPickerStyle {
    @ColorRes
    var nextButtonColor: Int = SceytKitConfig.sceytColorAccent

    @ColorRes
    var counterColor: Int = SceytKitConfig.sceytColorAccent

    @DrawableRes
    var checkedStateIcon: Int = R.drawable.ic_gallery_checked_state

    @DrawableRes
    var unCheckedStateIcon: Int = R.drawable.ic_gallery_unchecked_state

    var maxSelectCount: Int = 20
}